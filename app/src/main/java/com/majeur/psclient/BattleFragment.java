package com.majeur.psclient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.majeur.psclient.io.DataLoader;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.io.DexPokemonLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.io.MoveDetailsLoader;
import com.majeur.psclient.model.BasePokemon;
import com.majeur.psclient.model.BattleActionRequest;
import com.majeur.psclient.model.BattlingPokemon;
import com.majeur.psclient.model.Colors;
import com.majeur.psclient.model.Condition;
import com.majeur.psclient.model.DexPokemon;
import com.majeur.psclient.model.Move;
import com.majeur.psclient.model.Player;
import com.majeur.psclient.model.PokemonId;
import com.majeur.psclient.model.SidePokemon;
import com.majeur.psclient.model.StatModifiers;
import com.majeur.psclient.model.Stats;
import com.majeur.psclient.service.BattleMessageObserver;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.util.AudioBattleManager;
import com.majeur.psclient.util.BackForthTranslateAnimation;
import com.majeur.psclient.util.CircularTranslateAnimation;
import com.majeur.psclient.util.ShakeTranslateAnimation;
import com.majeur.psclient.util.Utils;
import com.majeur.psclient.widget.BattleActionWidget;
import com.majeur.psclient.widget.BattleLayout;
import com.majeur.psclient.widget.BattleTipPopup;
import com.majeur.psclient.widget.PlayerInfoView;
import com.majeur.psclient.widget.SideView;
import com.majeur.psclient.widget.StatusView;
import com.majeur.psclient.widget.ToasterView;

import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.util.Utils.array;
import static com.majeur.psclient.util.Utils.boldText;
import static com.majeur.psclient.util.Utils.italicText;
import static com.majeur.psclient.util.Utils.smallText;
import static com.majeur.psclient.util.Utils.tagText;
import static com.majeur.psclient.util.Utils.toStringSigned;

public class BattleFragment extends Fragment implements MainActivity.Callbacks {

    private TextView mLogTextView;
    private ScrollView mLogScrollView;
    private BattleLayout mBattleLayout;
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
    private AudioBattleManager mAudioManager;

    private BattlingPokemon[] mPlayerPokemons;
    private BattlingPokemon[] mFoePokemons;
    private BattleActionRequest mLastActionRequest;
    private boolean mTimerEnabled;
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
        mTrainerInfoView = view.findViewById(R.id.player1_info_view);
        mFoeInfoView = view.findViewById(R.id.player2_info_view);
        mActionWidget = view.findViewById(R.id.battle_action_widget);
        mBattleMessageView = view.findViewById(R.id.battle_message_view);
        mExtraActionsContainer = view.findViewById(R.id.extra_action_container);
        mExtraUndoContainer = view.findViewById(R.id.extra_undo_container);
        mExtraActionsContainer.animate().setInterpolator(new OvershootInterpolator(1.4f)).setDuration(500);

        final ImageView imageView = view.findViewById(R.id.overlay_image_view);
        imageView.setAlpha(0.65f);

        mTrainerInfoView.setOnClickListener(new View.OnClickListener() {

            boolean b = false;
            @Override
            public void onClick(View view) {
//                if (!b)
//                    Glide.with(view).load(R.raw.weather_hail).transition(withCrossFade()).into(imageView);
//                else
//                    Glide.with(view).load(R.raw.weather_sunny).transition(withCrossFade()).into(imageView);
//                b = !b;
            }
        });

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
        titleView.setText(pokemon.name + " " + Objects.toString(pokemon.gender, "") + " l." + pokemon.level);

        descView.setText(smallText("HP: "));
        String healthText = String.format("%.1f%% ", pokemon.condition.health * 100);
        descView.append(boldText(healthText, Colors.healthColor(pokemon.condition.health)));

        if (!pokemon.foe)
            descView.append(smallText("(" + pokemon.condition.hp + "/" + pokemon.condition.maxHp + ")"));

        if (pokemon.condition.status != null)
            descView.append(smallText(tagText(pokemon.condition.status.toUpperCase(),
                    Colors.statusColor(pokemon.condition.status))));

        descView.append("\n");

        if (!pokemon.foe) {
            SidePokemon sidePokemon = mLastActionRequest.getSide().get(0);
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
        }

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
                mSpritesLoader.loadTypeSprite(dexPokemon.firstType, placeHolderTop);
                if (dexPokemon.secondType != null)
                    mSpritesLoader.loadTypeSprite(dexPokemon.secondType, placeHolderBottom);

                if (!pokemon.foe) return;

                if (dexPokemon.abilities.size() > 1) {
                    descView.append(smallText("Possible abilities: "));
                    for (String ability : dexPokemon.abilities)
                        descView.append(ability + ", ");
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
        titleView.setText(move.name);

        descView.setText("PP: " + move.pp + "/" + move.ppMax);
        descView.append("\n");

        if (move.extraInfo == null)
            return;

        if (move.extraInfo.priority != 0) {
            descView.append(boldText("Priority: " + toStringSigned(move.extraInfo.priority)));
            descView.append("\n");
        }

        if (move.extraInfo.basePower != 0) {
            descView.append("Base power: " + move.extraInfo.basePower);
            descView.append("\n");
        }
        descView.append("Accuracy: " + (move.extraInfo.accuracy == -1 ? "-" : move.extraInfo.accuracy));

        if (move.extraInfo.desc != null) {
            descView.append("\n");
            descView.append(italicText(move.extraInfo.desc));
        }

        placeHolderTop.setImageDrawable(null);
        placeHolderBottom.setImageDrawable(null);
        mSpritesLoader.loadTypeSprite(move.extraInfo.type, placeHolderTop);
        mSpritesLoader.loadCategorySprite(move.extraInfo.category, placeHolderBottom);
    }

    private void bindSidePokemonPopup(SidePokemon pokemon, final TextView titleView,
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


        descView.append("Moves:");
        for (String move : pokemon.moves) {
            descView.append("\n\t");
            descView.append(move);
        }

        placeHolderTop.setImageDrawable(null);
        placeHolderBottom.setImageDrawable(null);
        mDexPokemonLoader.load(array(toId(pokemon.species)), new DataLoader.Callback<DexPokemon>() {
            @Override
            public void onLoaded(DexPokemon[] results) {
                DexPokemon dexPokemon = results[0];
                mSpritesLoader.loadTypeSprite(dexPokemon.firstType, placeHolderTop);
                if (dexPokemon.secondType != null)
                    mSpritesLoader.loadTypeSprite(dexPokemon.secondType, placeHolderBottom);
            }
        });
    }

    public void forfeit() {
        mService.sendRoomCommand(mObservedRoomId, "forfeit");
    }

    public void sendTeamDecision(int reqId, int first, int teamSize) {
        String teamOrder = "";
        for (int i = 1; i <= teamSize; i++) teamOrder += i;
        teamOrder = teamOrder.substring(first - 1) + teamOrder.substring(0, first - 1);
        mService.sendRoomCommand(mObservedRoomId, "team", teamOrder, reqId);
    }

    public void sendSwitchDecision(int reqId, int who) {
        mService.sendRoomCommand(mObservedRoomId, "switch", who, reqId);
    }

    public void sendMoveDecision(int reqId, int which, boolean mega, boolean zmove) {
        if (mega)
            mService.sendRoomCommand(mObservedRoomId, "move", which + " mega", reqId);
        else if (zmove)
            mService.sendRoomCommand(mObservedRoomId, "move", which + " zmove", reqId);
        else
            mService.sendRoomCommand(mObservedRoomId, "move", which, reqId);
    }

    public void sendTimerCommand(boolean on) {
        mService.sendRoomCommand(mObservedRoomId, "timer", on ? "on" : "off");
    }

    public void sendUndoCommand() {
        mService.sendRoomCommand(mObservedRoomId, "undo");
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
            ((MainActivity) getContext()).setKeepScreenOn(true);
            switch (getGameType()) {
                case SINGLE:
                    mBattleLayout.setMode(BattleLayout.MODE_BATTLE_SINGLE);
                    mPlayerPokemons = new BattlingPokemon[1];
                    mFoePokemons = new BattlingPokemon[1];
                    break;
                case DOUBLE:
                    mBattleLayout.setMode(BattleLayout.MODE_BATTLE_DOUBLE);
                    mPlayerPokemons = new BattlingPokemon[2];
                    mFoePokemons = new BattlingPokemon[2];
                    break;
                case TRIPLE:
                    mBattleLayout.setMode(BattleLayout.MODE_BATTLE_TRIPLE);
                    break;
            }
            mAudioManager.playBattleMusic();
            //sendChatMessage("[Playing from the unofficial Android Showdown client]");
            mBattleLayout.getSideView(Player.TRAINER).clearAllSides();
            mBattleLayout.getSideView(Player.FOE).clearAllSides();
        }

        @Override
        protected void onBattleEnded() {
            ((MainActivity) getContext()).setKeepScreenOn(false);
            mAudioManager.stopBattleMusic();
        }

        @Override
        protected void onPreviewStarted() {
            mBattleLayout.setMode(BattleLayout.MODE_PREVIEW);
        }

        @Override
        protected void onAddPreviewPokemon(final PokemonId id, final BasePokemon pokemon, boolean hasItem) {
            // Make sure BattleLayout has finished changing its layout
            mBattleLayout.post(new Runnable() {
                @Override
                public void run() {
                    ImageView imageView = mBattleLayout.getPokemonView(id);
                    if (imageView != null) // Happens when joining a battle where the preview has already been done
                        mSpritesLoader.loadPreviewSprite(id.player, pokemon, imageView);
                }
            });

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

//            mAudioManager.playPokemonCry(id.foe ? mFoePokemons[id.position] : mPlayerPokemons[id.position]);
        }

        @Override
        protected void onMove(final PokemonId sourceId, final PokemonId targetId, String moveName, boolean shouldAnim) {
            if (!shouldAnim) return;
            mMoveDetailsLoader.load(array(toId(moveName)), new DataLoader.Callback<Move.ExtraInfo>() {
                @Override
                public void onLoaded(Move.ExtraInfo[] results) {
                    int color = results[0].color;
                    String category = toId(results[0].category);
                    boolean selfAttack = sourceId.equals(targetId) || targetId == null;
                    View sourceView = mBattleLayout.getPokemonView(sourceId);
                    View targetView = !selfAttack ? mBattleLayout.getPokemonView(targetId) : null;
                    Animation sourceAnimation;
                    Animation targetAnimation;
                    int circleRadius = Utils.dpToPx(10);
                    int shakeAmplitude = Utils.dpToPx(8);
                    if (selfAttack) {
                        sourceAnimation = new CircularTranslateAnimation(circleRadius, 1.5f);
                        sourceAnimation.setDuration(1000);
                        sourceView.startAnimation(sourceAnimation);
                        return;
                    }
                    switch (category) {
                        case "status":
                            sourceAnimation = new CircularTranslateAnimation(circleRadius, 1.5f);
                            sourceAnimation.setDuration(1000);
                            sourceView.startAnimation(sourceAnimation);
                            targetAnimation = new ShakeTranslateAnimation(shakeAmplitude, 4f);
                            targetAnimation.setDuration(1000);
                            targetAnimation.setStartOffset(500);
                            targetView.startAnimation(targetAnimation);
                            break;
                        case "physical":
                        case "special":
                            sourceAnimation = new BackForthTranslateAnimation(
                                    sourceView.getX() + sourceView.getWidth() / 2f,
                                    sourceView.getY() + sourceView.getHeight() / 2f,
                                    targetView.getX() + targetView.getWidth() / 2f,
                                    targetView.getY() + targetView.getHeight() / 2f);
                            sourceAnimation.setDuration(750);
                            sourceAnimation.setStartOffset(250);
                            sourceView.startAnimation(sourceAnimation);

                            targetAnimation = new ShakeTranslateAnimation(shakeAmplitude, 4f);
                            targetAnimation.setDuration(750);
                            targetAnimation.setStartOffset(500);
                            targetView.startAnimation(targetAnimation);
                            break;
                    }
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

            if (!pokemon.foe)
                mPlayerPokemons[pokemon.position] = pokemon;
            else
                mFoePokemons[pokemon.position] = pokemon;

            mDexIconLoader.load(array(toId(pokemon.species)), new DataLoader.Callback<Bitmap>() {
                @Override
                public void onLoaded(Bitmap[] results) {
                    Drawable icon = new BitmapDrawable(results[0]);
                    PlayerInfoView infoView = !pokemon.foe ? mTrainerInfoView : mFoeInfoView;
                    infoView.updatePokemon(pokemon, icon);
                }
            });

//            mAudioManager.playPokemonCry(pokemon);
        }

        @Override
        protected void onDetailsChanged(final BattlingPokemon pokemon) {
            ImageView imageView = mBattleLayout.getPokemonView(pokemon.id);
            mSpritesLoader.loadSprite(pokemon, imageView, mBattleLayout.getWidth());

            if (!pokemon.foe) {
                mPlayerPokemons[pokemon.position].species = pokemon.species;
                mPlayerPokemons[pokemon.position].baseSpecies = pokemon.baseSpecies;
                mPlayerPokemons[pokemon.position].forme = pokemon.forme;
                mPlayerPokemons[pokemon.position].spriteId = pokemon.spriteId;
            } else {
                mFoePokemons[pokemon.position].species = pokemon.species;
                mFoePokemons[pokemon.position].baseSpecies = pokemon.baseSpecies;
                mFoePokemons[pokemon.position].forme = pokemon.forme;
                mFoePokemons[pokemon.position].spriteId = pokemon.spriteId;
            }

            mDexIconLoader.load(array(toId(pokemon.species)), new DataLoader.Callback<Bitmap>() {
                @Override
                public void onLoaded(Bitmap[] results) {
                    Drawable icon = new BitmapDrawable(results[0]);
                    PlayerInfoView infoView = !pokemon.foe ? mTrainerInfoView : mFoeInfoView;
                    infoView.updatePokemon(pokemon, icon);
                }
            });
        }

        @Override
        protected void onHealthChanged(PokemonId id, Condition condition) {
            StatusView statusView = mBattleLayout.getStatusView(id);
            statusView.setHealth(condition.health);

            ToasterView toasterView = mBattleLayout.getToasterView(id);
            toasterView.makeToast(condition.hp + "/" + condition.maxHp, Color.RED);

            if (!id.foe)
                mPlayerPokemons[id.position].condition = condition;
            else
                mFoePokemons[id.position].condition = condition;
        }

        @Override
        protected void onStatusChanged(PokemonId id, String status) {
            StatusView statusView = mBattleLayout.getStatusView(id);
            statusView.setStatus(status);

            if (!id.foe)
                mPlayerPokemons[id.position].condition.status = status;
            else
                mFoePokemons[id.position].condition.status = status;
        }

        @Override
        protected void onStatChanged(PokemonId id, String stat, int amount, boolean set) {
            StatModifiers statModifiers;
            if (id.foe)
                statModifiers = mFoePokemons[id.position].statModifiers;
            else
                statModifiers = mPlayerPokemons[id.position].statModifiers;

            if (set)
                statModifiers.set(stat, amount);
            else
                statModifiers.inc(stat, amount);
            StatusView statusView = mBattleLayout.getStatusView(id);
            statusView.updateModifier(statModifiers);
        }

        @Override
        protected void onRequestAsked(final BattleActionRequest request) {
            mLastActionRequest = request;
            if (request.shouldWait())
                return;

            boolean hideMoves = request.forceSwitch();
            boolean hideSwitch = request.trapped();
            List<Move> moves = hideMoves ? null : request.getMoves();
            List<SidePokemon> team = hideSwitch ? null : request.getSide();

            mActionWidget.promptChoice(mBattleTipPopup, moves, request.canMegaEvo(), team, request.teamPreview(),
                    new BattleActionWidget.OnChoiceListener() {
                @Override
                public void onMoveChose(int which, boolean mega, boolean zmove) {
                    sendMoveDecision(request.getId(), which, mega, zmove);
                }

                @Override
                public void onSwitchChose(int who) {
                    if (request.teamPreview())
                        sendTeamDecision(request.getId(), who, request.getSide().size());
                    else
                        sendSwitchDecision(request.getId(), who);
                }
            });

            if (!hideMoves && moves != null && moves.size() > 0) {
                String[] keys = new String[moves.size()];
                for (int i = 0; i < keys.length; i++) keys[i] = toId(moves.get(i).id);
                mMoveDetailsLoader.load(keys, new DataLoader.Callback<Move.ExtraInfo>() {
                    @Override
                    public void onLoaded(Move.ExtraInfo[] results) {
                        mActionWidget.updateMoveExtras(results);
                    }
                });
            }

            if (!hideSwitch) {
                String[] species = new String[team.size()];
                for (int i = 0; i < species.length; i++) species[i] = toId(team.get(i).species);
                mDexIconLoader.load(species, new DataLoader.Callback<Bitmap>() {
                    @Override
                    public void onLoaded(Bitmap[] results) {
                        Drawable[] icons = new Drawable[results.length];
                        for (int i = 0; i < icons.length; i++)
                            icons[i] = new BitmapDrawable(results[i]);
                        mActionWidget.updateDexIcons(icons);
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
            if (mLogTextView.getText().length() > 0)
                mLogTextView.append("\n");
            mLogTextView.append(text);
            mLogScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mLogScrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
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
            mLogTextView.setText("");
            mLastActionRequest = null;
            mActionWidget.dismissNow();
            onTimerEnabled(false);
        }

        @Override
        public void onRoomDeInit() {
            super.onRoomDeInit();
            // Ignored
        }
    };
}
