package com.majeur.psclient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.majeur.psclient.io.DataLoader;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.io.DexPokemonLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.io.ItemLoader;
import com.majeur.psclient.io.MoveDetailsLoader;
import com.majeur.psclient.model.BasePokemon;
import com.majeur.psclient.model.BattleActionRequest;
import com.majeur.psclient.model.BattleDecision;
import com.majeur.psclient.model.BattlingPokemon;
import com.majeur.psclient.model.Colors;
import com.majeur.psclient.model.Condition;
import com.majeur.psclient.model.DexPokemon;
import com.majeur.psclient.model.Item;
import com.majeur.psclient.model.Move;
import com.majeur.psclient.model.Player;
import com.majeur.psclient.model.PokemonId;
import com.majeur.psclient.model.SidePokemon;
import com.majeur.psclient.model.StatModifiers;
import com.majeur.psclient.model.Stats;
import com.majeur.psclient.model.Type;
import com.majeur.psclient.model.Weather;
import com.majeur.psclient.service.BattleMessageObserver;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.util.AudioBattleManager;
import com.majeur.psclient.util.Callback;
import com.majeur.psclient.util.CategoryDrawable;
import com.majeur.psclient.util.InactiveBattleOverlayDrawable;
import com.majeur.psclient.util.Preferences;
import com.majeur.psclient.util.Utils;
import com.majeur.psclient.util.html.Html;
import com.majeur.psclient.widget.BattleActionWidget;
import com.majeur.psclient.widget.BattleLayout;
import com.majeur.psclient.widget.BattleTipPopup;
import com.majeur.psclient.widget.PlayerInfoView;
import com.majeur.psclient.widget.SideView;
import com.majeur.psclient.widget.StatusView;
import com.majeur.psclient.widget.ToasterView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.model.Id.toIdSafe;
import static com.majeur.psclient.util.Utils.array;
import static com.majeur.psclient.util.Utils.boldText;
import static com.majeur.psclient.util.Utils.italicText;
import static com.majeur.psclient.util.Utils.notAllNull;
import static com.majeur.psclient.util.Utils.replace;
import static com.majeur.psclient.util.Utils.smallText;
import static com.majeur.psclient.util.Utils.str;
import static com.majeur.psclient.util.Utils.tagText;
import static com.majeur.psclient.util.Utils.toStringSigned;

@SuppressLint({"DefaultLocale", "SetTextI18n"})
public class BattleFragment extends Fragment implements MainActivity.Callbacks {

    private TextView mLogTextView;
    private ScrollView mLogScrollView;
    private BattleLayout mBattleLayout;
    private ImageView mOverlayImageView;
    private ImageView mBackgroundImageView;
    private PlayerInfoView mTrainerInfoView;
    private PlayerInfoView mFoeInfoView;
    private BattleActionWidget mActionWidget;
    private TextView mBattleMessageView;
    private View mExtraActionsContainer;
    private ImageButton mTimerButton;
    private View mExtraUndoContainer;
    private ImageButton mUndoButton;

    private ShowdownService mService;
    private GlideHelper mSpritesLoader;
    private BattleTipPopup mBattleTipPopup;
    private DexPokemonLoader mDexPokemonLoader;
    private MoveDetailsLoader mMoveDetailsLoader;
    private DexIconLoader mDexIconLoader;
    private ItemLoader mItemLoader;
    private AudioBattleManager mAudioManager;

    private InactiveBattleOverlayDrawable mInactiveBattleOverlayDrawable;
    private BattleActionRequest mLastActionRequest;
    private boolean mTimerEnabled;
    private boolean mSoundEnabled;
    private boolean mWasPlayingBattleMusicWhenPaused;

    private String mObservedRoomId;

    public String getObservedRoomId() {
        return mObservedRoomId;
    }

    public void setObservedRoomId(String observedRoomId) {
        mObservedRoomId = observedRoomId;
        mObserver.observeForRoomId(observedRoomId);
    }

    public boolean battleRunning() {
        return mObserver.battleRunning();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mObserver.gotContext(context);
        mSpritesLoader = ((MainActivity) context).getGlideHelper();
        mBattleTipPopup = new BattleTipPopup(context);
        mBattleTipPopup.setOnBindPopupViewListener(mOnBindPopupViewListener);
        mDexPokemonLoader = new DexPokemonLoader(context);
        mMoveDetailsLoader = new MoveDetailsLoader(context);
        mDexIconLoader = ((MainActivity) context).getDexIconLoader();
        mItemLoader = new ItemLoader(context);
        mAudioManager = new AudioBattleManager(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_battle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLogTextView = view.findViewById(R.id.text_view_log);
        mLogScrollView = view.findViewById(R.id.scroll_view_log);
        mBattleLayout = view.findViewById(R.id.battle_layout);
        mOverlayImageView = view.findViewById(R.id.overlay_image_view);
        mBackgroundImageView = view.findViewById(R.id.background_image_view);
        mTrainerInfoView = view.findViewById(R.id.player1_info_view);
        mFoeInfoView = view.findViewById(R.id.player2_info_view);
        mActionWidget = view.findViewById(R.id.battle_action_widget);
        mBattleMessageView = view.findViewById(R.id.battle_message_view);
        mExtraActionsContainer = view.findViewById(R.id.extra_action_container);
        mExtraUndoContainer = view.findViewById(R.id.extra_undo_container);
        mExtraActionsContainer.animate().setInterpolator(new OvershootInterpolator(1.4f)).setDuration(500);

        mInactiveBattleOverlayDrawable = new InactiveBattleOverlayDrawable(getResources());
        mOverlayImageView.setImageDrawable(mInactiveBattleOverlayDrawable);

        mActionWidget.setOnRevealListener(new BattleActionWidget.OnRevealListener() {
            @Override
            public void onReveal(boolean in) {
                mExtraActionsContainer.animate()
                        .setStartDelay(in ? 0 : 350)
                        .translationY(in ? 3 * mLogScrollView.getHeight() / 5 : 0)
                        .start();

                if (in) {
                    mExtraUndoContainer.setTranslationX(mExtraActionsContainer.getWidth());
                    mExtraUndoContainer.setAlpha(0);
                } else {
                    mUndoButton.setEnabled(true);
                    mExtraUndoContainer.animate()
                            .setStartDelay(350)
                            .translationX(0)
                            .alpha(1f)
                            .start();
                }
            }
        });

        mTimerButton = view.findViewById(R.id.button_timer);
        mTimerButton.setOnClickListener(mExtraClickListener);
        view.findViewById(R.id.button_forfeit).setOnClickListener(mExtraClickListener);
        view.findViewById(R.id.button_send).setOnClickListener(mExtraClickListener);
        mUndoButton = view.findViewById(R.id.button_undo);
        mUndoButton.setOnClickListener(mExtraClickListener);

        mExtraUndoContainer.post(new Runnable() {
            @Override
            public void run() {
                mExtraUndoContainer.setTranslationX(mExtraActionsContainer.getWidth());
                mExtraUndoContainer.setAlpha(0);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAudioManager.isPlayingBattleMusic()) {
            mAudioManager.pauseBattleMusic();
            mWasPlayingBattleMusicWhenPaused = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mWasPlayingBattleMusicWhenPaused) {
            mAudioManager.playBattleMusic();
            mWasPlayingBattleMusicWhenPaused = false;
        }
    }

    @Override
    public void onServiceBound(ShowdownService service) {
        mService = service;
        service.registerMessageObserver(mObserver, false);
    }

    @Override
    public void onServiceWillUnbound(ShowdownService service) {
        mService = null;
        service.unregisterMessageObserver(mObserver);
    }

    private void prepareBattleFieldUi() {
        mBattleLayout.setAlpha(1f);
        mOverlayImageView.setAlpha(1f);
        mOverlayImageView.setImageDrawable(null);
    }

    private void clearBattleFieldUi() {
        mBattleLayout.animate().alpha(0f).withEndAction(new Runnable() {
            @Override
            public void run() {
                mBattleLayout.getSideView(Player.TRAINER).clearAllSides();
                mBattleLayout.getSideView(Player.FOE).clearAllSides();
                mOverlayImageView.setImageDrawable(null);

                mTrainerInfoView.clear();
                mFoeInfoView.clear();

                mOverlayImageView.setAlpha(0f);
                mOverlayImageView.setImageDrawable(mInactiveBattleOverlayDrawable);
                mOverlayImageView.animate().alpha(1f);
            }
        }).start();
    }

    private final View.OnClickListener mExtraClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            switch (id) {
                case R.id.button_forfeit:
                    if (!battleRunning()) break;
                    new AlertDialog.Builder(getContext())
                            .setMessage("Do you really want to forfeit this battle ?")
                            .setPositiveButton("Forfeit", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    forfeit();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    break;
                case R.id.button_send:
                    if (mObserver.observedRoomId() == null) break;
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_battle_message, null);
                    final EditText editText = dialogView.findViewById(R.id.edit_text_team_name);
                    new MaterialAlertDialogBuilder(getContext())
                            .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String input = editText.getText().toString();
                                    String regex = "[{}:\",|\\[\\]]";
                                    if (input.matches(".*" + regex + ".*")) input = input.replaceAll(regex, "");
                                    mService.sendRoomMessage(mObservedRoomId, input);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .setNeutralButton("\"gg\"", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    mService.sendRoomMessage(mObservedRoomId, "gg");
                                }
                            })
                            .setView(dialogView)
                            .show();
                    editText.requestFocus();
                    break;
                case R.id.button_timer:
                    if (!battleRunning()) break;
                    sendTimerCommand(!mTimerEnabled);
                    break;
                case R.id.button_undo:
                    mUndoButton.setEnabled(false);
                    sendUndoCommand();
                    mObserver.reAskForRequest();
                    break;
            }
        }
    };

    private final BattleTipPopup.OnBindPopupViewListener mOnBindPopupViewListener = new BattleTipPopup.OnBindPopupViewListener() {
        @Override
        public void onBindPopupView(View anchorView, TextView titleView, TextView descView,
                                    ImageView placeHolderTop, ImageView placeHolderBottom) {
            Object data = anchorView.getTag(R.id.battle_data_tag);
            if (data instanceof BattlingPokemon)
                bindBattlingPokemonTipPopup((BattlingPokemon) data, titleView, descView, placeHolderTop, placeHolderBottom);
            else if (data instanceof Move)
                bindMoveTipPopup((Move) data, titleView, descView, placeHolderTop, placeHolderBottom);
            else if (data instanceof SidePokemon)
                bindSidePokemonPopup((SidePokemon) data, titleView, descView, placeHolderTop, placeHolderBottom);
        }
    };

    private void bindBattlingPokemonTipPopup(final BattlingPokemon pokemon, final TextView titleView,
                                             final TextView descView, final ImageView placeHolderTop,
                                             final ImageView placeHolderBottom) {
        titleView.setText(pokemon.name);
        titleView.append(" ");
        titleView.append(smallText(pokemon.gender));
        titleView.append(" l.");
        titleView.append(str(pokemon.level));

        descView.setText("");
        if (!pokemon.species.equals(pokemon.name)) descView.append(pokemon.species + "\n");
        descView.append(smallText("HP: "));
        String healthText = String.format("%.1f%% ", pokemon.condition.health * 100);
        descView.append(boldText(healthText, Colors.healthColor(pokemon.condition.health)));
        if (!pokemon.foe)
            descView.append(smallText("(" + pokemon.condition.hp + "/" + pokemon.condition.maxHp + ")"));
        if (pokemon.condition.status != null)
            descView.append(smallText(tagText(pokemon.condition.status.toUpperCase(),
                    Colors.statusColor(pokemon.condition.status))));

        descView.append("\n");
        final String ability;
        if (!pokemon.foe && mLastActionRequest != null) {
            final SidePokemon sidePokemon = mLastActionRequest.getSide().get(0);
            ability = sidePokemon.ability;
            descView.append(smallText("Atk:"));
            descView.append(pokemon.statModifiers.calcReadableStat("atk", sidePokemon.stats.atk));
            descView.append(smallText(" Def:"));
            descView.append(pokemon.statModifiers.calcReadableStat("def", sidePokemon.stats.def));
            descView.append(smallText(" Spa:"));
            descView.append(pokemon.statModifiers.calcReadableStat("spa", sidePokemon.stats.spa));
            descView.append(smallText(" Spd:"));
            descView.append(pokemon.statModifiers.calcReadableStat("spd", sidePokemon.stats.spd));
            descView.append(smallText(" Spe:"));
            descView.append(pokemon.statModifiers.calcReadableStat("spe", sidePokemon.stats.spe));
            descView.append("\n");
            descView.append(smallText("Ability: "));
            descView.append(sidePokemon.ability);
            descView.append("\n");
            descView.append(smallText("Item: "));
            descView.append(sidePokemon.item);
            mItemLoader.load(array(toId(sidePokemon.item)), new DataLoader.Callback<Item>() {
                @Override
                public void onLoaded(Item[] results) {
                    if (results[0] != null)
                        replace(descView.getEditableText(), sidePokemon.item, results[0].name);
                }
            });

        } else ability = null;
        placeHolderTop.setImageDrawable(null);
        placeHolderBottom.setImageDrawable(null);
        mDexPokemonLoader.load(array(toId(pokemon.species)), new DataLoader.Callback<DexPokemon>() {
            @Override
            public void onLoaded(DexPokemon[] results) {
                DexPokemon dexPokemon = results[0];
                if (dexPokemon == null) {
                    descView.append("No dex entry for " + pokemon.species);
                    return;
                }
                placeHolderTop.setImageResource(Type.getResId(dexPokemon.firstType));
                if (dexPokemon.secondType != null)
                    placeHolderBottom.setImageResource(Type.getResId(dexPokemon.secondType));
                if (!pokemon.foe) {
                    if (ability == null) return;
                    String abilityName = null;
                    if (ability.equals(toIdSafe(dexPokemon.hiddenAbility)))
                        abilityName = dexPokemon.hiddenAbility;
                    else for (String ab : dexPokemon.abilities)
                        if (toId(ab).equals(ability))
                            abilityName = ab;
                    if (abilityName != null)
                        replace(descView.getEditableText(), ability, abilityName);
                    return;
                }
                if (dexPokemon.abilities.size() > 1 || dexPokemon.hiddenAbility != null) {
                    descView.append(smallText("Possible abilities: "));
                    for (String ability : dexPokemon.abilities)
                        descView.append(ability + ", ");
                    descView.append(dexPokemon.hiddenAbility);
                    descView.append("\n");
                } else if (dexPokemon.abilities.size() > 0) {
                    descView.append(smallText("Ability: "));
                    descView.append(dexPokemon.abilities.get(0));
                    descView.append("\n");
                }
                int[] speedRange = Stats.calculateSpeedRange(pokemon.level, dexPokemon.baseStats.spe, "Random Battle", 7);
                descView.append(smallText("Speed: "));
                descView.append(speedRange[0] + " to " + speedRange[1]);
                descView.append(smallText(" (before items/abilities/modifiers)"));
            }
        });
    }

    private void bindMoveTipPopup(Move move, TextView titleView, TextView descView, ImageView placeHolderTop,
                                  ImageView placeHolderBottom) {
        String moveName = null;
        if (move.maxflag) moveName = move.maxDetails != null ? move.maxDetails.name : move.maxMoveId;
        if (move.zflag) moveName = move.zName;
        if (moveName == null) moveName = move.name;
        titleView.setText(moveName);

        descView.setText("");
        int priority = -20;
        if (move.maxflag) priority = move.maxDetails != null ? move.maxDetails.priority : 0;
        if (move.zflag) priority = move.zDetails != null ? move.zDetails.priority : 0;
        if (priority == -20) priority = move.details != null ? move.details.priority : 0;
        if (priority > 1) {
            descView.append("Nearly always moves first ");
            descView.append(italicText("(priority " + toStringSigned(priority) + ")\n"));
        } else if (priority <= -1) {
            descView.append("Nearly always moves last ");
            descView.append(italicText("(priority " + toStringSigned(priority) + ")\n"));
        } else if (priority == 1) {
            descView.append("Usually moves first ");
            descView.append(italicText("(priority " + toStringSigned(priority) + ")\n"));
        }

        int basePower = -1;
        if (move.maxflag) basePower = move.details != null ? move.details.maxPower : 0;
        if (move.zflag) basePower = move.zDetails != null ? move.zDetails.basePower :
                move.details != null ? move.details.zPower : 0;
        if (basePower == -1) basePower = move.details != null ? move.details.basePower : 0;
        if (basePower > 0)
            descView.append("Base power: " + basePower + "\n");

        int accuracy = -20;
        if (move.maxflag) accuracy = move.maxDetails != null ? move.maxDetails.accuracy : 0;
        if (move.zflag) accuracy = 0;
        if (accuracy == -20) accuracy = move.details != null ? move.details.accuracy : 0;
        if (accuracy != 0) {
            descView.append("Accuracy: ");
            if (accuracy == Integer.MAX_VALUE) descView.append("can't miss");
            else descView.append(str(accuracy));
            descView.append("\n");
        }

        String desc = null;
        if (move.maxflag) desc = move.maxDetails != null ? move.maxDetails.desc : "";
        if (move.zflag) desc = move.zDetails != null ? move.zDetails.desc :
                move.details != null ? "Z-Effect: " + move.details.zEffect : "";
        if (desc == null) desc = move.details != null ? move.details.desc : "";
        if (desc != null && desc.length() > 0)
            descView.append(italicText(desc));

        String type = null;
        if (move.maxflag) type = move.maxDetails != null ? move.maxDetails.type : "???";
        if (type == null) type = move.details != null ? move.details.type : "???";
        placeHolderTop.setImageResource(Type.getResId(type));

        String category = move.details != null ? move.details.category : null;
        Drawable drawable = category != null ? new CategoryDrawable(move.details.category) : null;
        placeHolderBottom.setImageDrawable(drawable);

//        if (move.zflag) {
//            titleView.setText(move.zName);
//            descView.setText("PP: 1/1");
//            descView.append("\n");
//            if (move.details != null && move.details.zPower > 0) {
//                descView.append("Base power: " + move.details.zPower);
//                descView.append("\n");
//            }
//            if (move.details != null && move.details.zEffect != null) {
//                descView.append("Z-Effect: " + move.details.zEffect);
//                descView.append("\n");
//            }
//            if (move.zDetails != null && move.zDetails.desc != null) {
//                descView.append(italicText(move.zDetails.desc));
//            } else if (move.details != null){
//                descView.append(italicText(move.details.desc));
//            }
//        } else if (move.maxflag) {
//            titleView.setText(move.maxDetails.name);
//            descView.setText(move.maxDetails.toString());
//
//            descView.append("\n");
//        }
//        if (move.details == null) return;
//        if (move.zflag) return;
//
//
//        if (move.details.basePower > 0) {
//            descView.append("Base power: " + move.details.basePower);
//            descView.append("\n");
//        }
//        descView.append("Accuracy: " + (move.details.accuracy > 0 ? move.details.accuracy : "Can't miss"));

    }

    private void bindSidePokemonPopup(final SidePokemon pokemon, final TextView titleView,
                                      final TextView descView, final ImageView placeHolderTop, final ImageView placeHolderBottom) {
        titleView.setText(pokemon.name);

        descView.setText(smallText("HP: "));
        String healthText = String.format("%.1f%% ", pokemon.condition.health * 100);
        descView.append(boldText(healthText, Colors.healthColor(pokemon.condition.health)));
        descView.append(smallText("(" + pokemon.condition.hp + "/" + pokemon.condition.maxHp + ")"));
        if (pokemon.condition.status != null)
            descView.append(smallText(tagText(pokemon.condition.status.toUpperCase(),
                    Colors.statusColor(pokemon.condition.status))));

        descView.append("\n");
        descView.append(smallText("Atk:"));
        descView.append(str(pokemon.stats.atk));
        descView.append(smallText(" Def:"));
        descView.append(str(pokemon.stats.def));
        descView.append(smallText(" Spa:"));
        descView.append(str(pokemon.stats.spa));
        descView.append(smallText(" Spd:"));
        descView.append(str(pokemon.stats.spd));
        descView.append(smallText(" Spe:"));
        descView.append(str(pokemon.stats.spe));
        descView.append("\n");
        descView.append(smallText("Ability: "));
        descView.append(pokemon.ability);
        descView.append("\n");
        descView.append(smallText("Item: "));
        descView.append(pokemon.item);
        mItemLoader.load(array(toId(pokemon.item)), new DataLoader.Callback<Item>() {
            @Override
            public void onLoaded(Item[] results) {
                if (results[0] != null)
                    replace(descView.getEditableText(), pokemon.item, results[0].name);
            }
        });
        descView.append("\n");

        descView.append(smallText("Moves:"));
        String[] query = new String[pokemon.moves.size()];
        for (int i = 0; i < query.length; i++)
            query[i] = toId(pokemon.moves.get(i));
        mMoveDetailsLoader.load(query, new DataLoader.Callback<Move.Details>() {
            @Override
            public void onLoaded(Move.Details[] results) {
                for (int i = 0; i < results.length; i++) {
                    descView.append("\n\t");
                    descView.append(results[i] != null ?
                            results[i].name : pokemon.moves.get(i));
                }
            }
        });

        placeHolderTop.setImageDrawable(null);
        placeHolderBottom.setImageDrawable(null);
        mDexPokemonLoader.load(array(toId(pokemon.species)), new DataLoader.Callback<DexPokemon>() {
            @Override
            public void onLoaded(DexPokemon[] results) {
                DexPokemon dexPokemon = results[0];
                if (dexPokemon == null) return;
                placeHolderTop.setImageResource(Type.getResId(dexPokemon.firstType));
                if (dexPokemon.secondType != null)
                    placeHolderBottom.setImageResource(Type.getResId(dexPokemon.secondType));

                if (pokemon.ability == null) return;
                String abilityName = null;
                if (pokemon.ability.equals(toIdSafe(dexPokemon.hiddenAbility)))
                    abilityName = dexPokemon.hiddenAbility;
                else for (String ab : dexPokemon.abilities)
                    if (toId(ab).equals(pokemon.ability))
                        abilityName = ab;
                if (abilityName != null)
                    replace(descView.getEditableText(), pokemon.ability, abilityName);
            }
        });
    }

    public void forfeit() {
        mService.sendRoomCommand(mObservedRoomId, "forfeit");
    }

    public void sendDecision(int reqId, BattleDecision decision) {
        mService.sendRoomCommand(mObservedRoomId, decision.getCommand(), decision.build(), reqId);
    }

    public void sendTimerCommand(boolean on) {
        mService.sendRoomCommand(mObservedRoomId, "timer", on ? "on" : "off");
    }

    public void sendUndoCommand() {
        mService.sendRoomCommand(mObservedRoomId, "undo");
    }

    private void notifyNewMessageReceived() {
        MainActivity activity = (MainActivity) getActivity();
        if (getId() != activity.getSelectedFragmentId())
            activity.showBadge(getId());
    }

    private BattleMessageObserver mObserver = new BattleMessageObserver() {

        @Override
        protected void onTimerEnabled(boolean enabled) {
            mTimerEnabled = enabled;
            int color = getResources().getColor(enabled ? R.color.accent : R.color.primary);
            mTimerButton.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        }

        @Override
        protected void onPlayerInit(String playerUsername, String foeUsername) {
            mTrainerInfoView.setUsername(playerUsername);
            mFoeInfoView.setUsername(foeUsername);
        }

        @Override
        protected void onBattleStarted() {
            prepareBattleFieldUi();
            ((MainActivity) getContext()).setKeepScreenOn(true);
            switch (getGameType()) {
                case SINGLE:
                    mBattleLayout.setMode(BattleLayout.MODE_BATTLE_SINGLE);
                    break;
                case DOUBLE:
                    mBattleLayout.setMode(BattleLayout.MODE_BATTLE_DOUBLE);
                    break;
                case TRIPLE:
                    mBattleLayout.setMode(BattleLayout.MODE_BATTLE_TRIPLE);
                    break;
            }
            if (mSoundEnabled) mAudioManager.playBattleMusic();
            //sendChatMessage("[Playing from the unofficial Android Showdown client]");
        }

        @Override
        protected void onBattleEnded(String winner) {
            ((MainActivity) getContext()).setKeepScreenOn(false);
            mAudioManager.stopBattleMusic();
            mInactiveBattleOverlayDrawable.setWinner(winner);
            clearBattleFieldUi();
        }

        @Override
        protected void onPreviewStarted() {
            prepareBattleFieldUi();
            mBattleLayout.setMode(BattleLayout.MODE_PREVIEW);
        }

        @Override
        protected void onAddPreviewPokemon(final PokemonId id, final BasePokemon pokemon, boolean hasItem) {
            ImageView imageView = mBattleLayout.getPokemonView(id);
            if (imageView != null) // Happens when joining a battle where the preview has already been done
                mSpritesLoader.loadPreviewSprite(id.player, pokemon, imageView);

            mDexIconLoader.load(array(toId(pokemon.species)), new DataLoader.Callback<Bitmap>() {
                @Override
                public void onLoaded(Bitmap[] results) {
                    Drawable icon = new BitmapDrawable(results[0]);
                    PlayerInfoView infoView = !id.foe ? mTrainerInfoView : mFoeInfoView;
                    infoView.appendPokemon(pokemon, icon);
                }
            });
        }

        @Override
        protected void onTeamSize(Player player, int size) {
            PlayerInfoView infoView = player == Player.TRAINER ? mTrainerInfoView : mFoeInfoView;
            infoView.setTeamSize(size);
            mBattleLayout.setPreviewTeamSize(player, size);
        }

        @Override
        protected void onFaint(PokemonId id) {
            final View pokemonView = mBattleLayout.getPokemonView(id);
            pokemonView.animate()
                    .setDuration(250)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .translationY(pokemonView.getHeight()/2)
                    .alpha(0f)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            pokemonView.setTranslationY(0);
                        }
                    })
                    .start();

            View statusView = mBattleLayout.getStatusView(id);
            statusView.animate().alpha(0f).start();

            PlayerInfoView playerView = id.foe ? mFoeInfoView : mTrainerInfoView;
            playerView.setPokemonFainted(getBattlingPokemon(id));

            if (mSoundEnabled) mAudioManager.playPokemonCry(getBattlingPokemon(id), true);
        }

        @Override
        protected void onMove(final PokemonId sourceId, final PokemonId targetId, String moveName, boolean shouldAnim) {
            if (!shouldAnim || targetId == null) return;
            mMoveDetailsLoader.load(array(moveName), new DataLoader.Callback<Move.Details>() {
                @Override
                public void onLoaded(Move.Details[] results) {
                    if (results[0] == null) return;
                    String category = toId(results[0].category);
                    if ("status".equals(category)) return;
                    mBattleLayout.displayHitIndicator(targetId);
                    //if (mSoundEnabled) mAudioManager.playMoveHitSound();
                }
            });
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void onSwitch(final BattlingPokemon pokemon) {
            final int pokemonPos = pokemon.position;
            if (pokemonPos < 0)
                // No battle index provided, pass
                return;

            StatusView statusView = mBattleLayout.getStatusView(pokemon.id);
            statusView.setPokemon(pokemon);
            statusView.animate().alpha(1f).start();

            ImageView imageView = mBattleLayout.getPokemonView(pokemon.id);
            imageView.setTag(R.id.battle_data_tag, pokemon);
            mBattleTipPopup.addTippedView(imageView);
            mSpritesLoader.loadSprite(pokemon, imageView, mBattleLayout.getWidth());

            mDexIconLoader.load(array(toId(pokemon.species)), new DataLoader.Callback<Bitmap>() {
                @Override
                public void onLoaded(Bitmap[] results) {
                    Drawable icon = new BitmapDrawable(results[0]);
                    PlayerInfoView infoView = !pokemon.foe ? mTrainerInfoView : mFoeInfoView;
                    infoView.updatePokemon(pokemon, icon);
                }
            });
            if (mSoundEnabled) mAudioManager.playPokemonCry(pokemon, false);
        }

        @Override
        protected void onDetailsChanged(final BattlingPokemon pokemon) {
            ImageView imageView = mBattleLayout.getPokemonView(pokemon.id);
            mSpritesLoader.loadSprite(pokemon, imageView, mBattleLayout.getWidth());

            mDexIconLoader.load(array(toId(pokemon.species)), new DataLoader.Callback<Bitmap>() {
                @Override
                public void onLoaded(Bitmap[] results) {
                    Drawable icon = new BitmapDrawable(results[0]);
                    PlayerInfoView infoView = !pokemon.foe ? mTrainerInfoView : mFoeInfoView;
                    infoView.updatePokemon(pokemon, icon);
                }
            });

            if (mSoundEnabled && "mega".equals(pokemon.forme)) mAudioManager.playPokemonCry(pokemon, false);
        }

        @Override
        protected void onHealthChanged(PokemonId id, Condition condition) {
            StatusView statusView = mBattleLayout.getStatusView(id);
            statusView.setHealth(condition.health);
        }

        @Override
        protected void onStatusChanged(PokemonId id, String status) {
            StatusView statusView = mBattleLayout.getStatusView(id);
            statusView.setStatus(status);
        }

        @Override
        protected void onStatChanged(PokemonId id, String stat, int amount, boolean set) {
            StatModifiers statModifiers = getBattlingPokemon(id).statModifiers;
            StatusView statusView = mBattleLayout.getStatusView(id);
            statusView.updateModifier(statModifiers);
        }

        @Override
        protected void onRequestAsked(final BattleActionRequest request) {
            mLastActionRequest = request;
            if (request.shouldWait()) return;
            mActionWidget.promptDecision(this, mBattleTipPopup, request, new BattleActionWidget.OnDecisionListener() {
                @Override
                public void onDecisionTook(BattleDecision decision) {
                    sendDecision(request.getId(), decision);
                }
            });

            boolean hideSwitch = true;
            for (int which = 0; which < request.getCount(); which++) {
                if (!request.trapped(which)) hideSwitch = false;

                boolean hideMoves = request.forceSwitch(which);
                final Move[] moves = hideMoves ? null : request.getMoves(which);
                if (hideMoves || moves == null || moves.length == 0) continue;

                String[] keys = new String[moves.length];
                for (int i = 0; i < keys.length; i++) keys[i] = moves[i].id;
                mMoveDetailsLoader.load(keys, new DataLoader.Callback<Move.Details>() {
                    @Override
                    public void onLoaded(Move.Details[] results) {
                        for (int i = 0; i < results.length; i++)
                            moves[i].details = results[i];
                        mActionWidget.notifyDetailsUpdated();
                    }
                });
                keys = new String[moves.length];
                for (int i = 0; i < keys.length; i++) keys[i] = toIdSafe(moves[i].zName);
                if (notAllNull(keys))
                    mMoveDetailsLoader.load(keys, new DataLoader.Callback<Move.Details>() {
                        @Override
                        public void onLoaded(Move.Details[] results) {
                            for (int i = 0; i < results.length; i++)
                                moves[i].zDetails = results[i];
                        }
                    });
                keys = new String[moves.length];
                for (int i = 0; i < keys.length; i++)
                    keys[i] = toIdSafe(moves[i].maxMoveId);
                if (notAllNull(keys))
                    mMoveDetailsLoader.load(keys, new DataLoader.Callback<Move.Details>() {
                        @Override
                        public void onLoaded(Move.Details[] results) {
                            for (int i = 0; i < results.length; i++)
                                moves[i].maxDetails = results[i];
                            mActionWidget.notifyMaxDetailsUpdated();
                        }
                    });
            }

            if (request.teamPreview()) hideSwitch = false;
            final List<SidePokemon> team = hideSwitch ? null : request.getSide();
            if (!hideSwitch) {
                String[] species = new String[team.size()];
                for (int i = 0; i < species.length; i++) species[i] = toId(team.get(i).species);
                mDexIconLoader.load(species, new DataLoader.Callback<Bitmap>() {
                    @Override
                    public void onLoaded(Bitmap[] results) {
                        for (int i = 0; i < team.size(); i++)
                            team.get(i).icon = results[i];
                        mActionWidget.notifyDexIconsUpdated();
                    }
                });
            }
        }

        @Override
        protected void onDisplayBattleToast(PokemonId id, String text, int color) {
            ToasterView toasterView = mBattleLayout.getToasterView(id);
            toasterView.makeToast(text, color);
        }

        @Override
        protected void onWeatherChanged(String weather) {
            final int resId = Weather.weatherResId(weather);
            if (Integer.valueOf(resId).equals(mOverlayImageView.getTag())) return;
            if (resId > 0) {
                mOverlayImageView.setAlpha(0f);
                mOverlayImageView.setImageResource(resId);
                mOverlayImageView.setTag(resId);
                mOverlayImageView.animate()
                        .alpha(0.75f)
                        .setDuration(250)
                        .withEndAction(null)
                        .start();
            } else {
                mOverlayImageView.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                mOverlayImageView.setImageDrawable(null);
                            }
                        })
                        .start();
            }
        }

        @Override
        protected void onSideChanged(Player player, String side, boolean start) {
            SideView sideView = mBattleLayout.getSideView(player);
            if (start) sideView.sideStart(side);
            else sideView.sideEnd(side);
        }

        @Override
        protected void onVolatileStatusChanged(PokemonId id, String vStatus, boolean start) {
            StatusView statusView = mBattleLayout.getStatusView(id);
            if (start)
                statusView.addVolatileStatus(vStatus);
            else
                statusView.removeVolatileStatus(vStatus);
        }

        @Override
        protected void onMarkBreak() {
            super.onMarkBreak();
            mActionWidget.dismiss();
            mUndoButton.setEnabled(false);
            mExtraUndoContainer.animate()
                    .setStartDelay(0)
                    .translationX(mExtraActionsContainer.getWidth())
                    .alpha(0f)
                    .start();
        }

        @Override
        public void onPrintText(CharSequence text) {
            boolean fullScrolled = Utils.fullScrolled(mLogScrollView);
            int l = mLogTextView.length();
            if (l > 0) mLogTextView.append("\n");
            mLogTextView.append(text);
            notifyNewMessageReceived();
            if (fullScrolled) postFullScroll();
        }

        @Override
        protected void onPrintBattleMessage(CharSequence text) {
            mBattleMessageView.setText(text);
            mBattleMessageView.animate().cancel();
            mBattleMessageView.setAlpha(1f);
            mBattleMessageView.animate()
                    .setDuration(1000)
                    .setStartDelay(750)
                    .alpha(0f)
                    .start();
        }

        @Override
        protected void onPrintHtml(String html) {
            final Object mark = new Object();
            int l = mLogTextView.length();
            mLogTextView.append("\u200C");
            mLogTextView.getEditableText().setSpan(mark, l, l + 1, Spanned.SPAN_MARK_MARK);
            Html.fromHtml(html,
                    Html.FROM_HTML_MODE_COMPACT,
                    mSpritesLoader.getHtmlImageGetter(mDexIconLoader, mLogTextView.getWidth()),
                    new Callback<Spanned>() {
                        @Override
                        public void callback(Spanned spanned) {
                            int at = mLogTextView.getEditableText().getSpanStart(mark);
                            if (at == -1) return; // Check if text has been cleared
                            boolean fullScrolled = Utils.fullScrolled(mLogScrollView);
                            mLogTextView.getEditableText()
                                    .insert(at, "\n")
                                    .insert(at + 1, spanned);
                            notifyNewMessageReceived();
                            if (fullScrolled) postFullScroll();
                        }
                    });
        }

        private void postFullScroll() {
            mLogScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mLogScrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        }

        @Override
        public void onRoomTitleChanged(String title) {
            // Ignored
        }

        @Override
        public void onUpdateUsers(List<String> users) {
            // Ignored
        }

        @Override
        public void onRoomInit() {
            super.onRoomInit();
            mSoundEnabled = Preferences.getBoolPreference(getContext(), "sound");
            mLogTextView.setText("", TextView.BufferType.EDITABLE);
            mLastActionRequest = null;
            mActionWidget.dismissNow();
            onTimerEnabled(false);
            mBackgroundImageView.animate()
                    .setDuration(100)
                    .alpha(0)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            int resId = Math.random() > 0.5 ? R.drawable.battle_bg_1
                                    : R.drawable.battle_bg_2;
                            mBackgroundImageView.setImageResource(resId);
                            mBackgroundImageView.animate()
                                    .setDuration(100)
                                    .alpha(1)
                                    .withEndAction(null)
                                    .start();
                        }
                    })
                    .start();
            // In case of corrupted battle stream make sure we stop music at the next one
            mAudioManager.stopBattleMusic();
        }

        @Override
        public void onRoomDeInit() {
            super.onRoomDeInit();
            ((MainActivity) getContext()).setKeepScreenOn(false);
            mAudioManager.stopBattleMusic();
            mInactiveBattleOverlayDrawable.setWinner(null);
            clearBattleFieldUi();
            mActionWidget.dismiss();
            mLogTextView.setText("");
            mLastActionRequest = null;
            mActionWidget.dismiss();
            mUndoButton.setEnabled(false);
            mExtraUndoContainer.animate()
                    .setStartDelay(0)
                    .translationX(mExtraActionsContainer.getWidth())
                    .alpha(0f)
                    .start();
            mTimerButton.getDrawable().clearColorFilter();
        }
    };
}
