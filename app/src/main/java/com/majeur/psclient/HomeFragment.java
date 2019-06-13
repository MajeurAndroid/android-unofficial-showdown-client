package com.majeur.psclient;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.majeur.psclient.io.DataLoader;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.model.AvailableBattleRoomsInfo;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.RoomInfo;
import com.majeur.psclient.model.Team;
import com.majeur.psclient.service.GlobalMessageObserver;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.widget.CategoryAdapter;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.majeur.psclient.model.Id.toId;

public class HomeFragment extends Fragment implements MainActivity.Callbacks {

    private DexIconLoader mDexIconLoader;

    private Dialog mCurrentDialog;
    private Button mBattleButton;

    private Spinner mFormatsSpinner;
    private Spinner mTeamsSpinner;
    private BattleFormat mCurrentBattleFormat;


    private String mCurrentUserName;

    private boolean mAutoJoinNextBattle;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mDexIconLoader = ((MainActivity) context).getDexIconLoader();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentDialog = ProgressDialog.show(getContext(),
                "Just a moment",
                "Connecting to Pokémon Showdown server...",
                true,
                false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mFormatsSpinner = view.findViewById(R.id.spinner_formats);
        mFormatsSpinner.setAdapter(new CategoryAdapter(getContext()) {
            @Override
            protected boolean isCategoryItem(int position) {
                return getItem(position) instanceof BattleFormat.Category;
            }

            @Override
            protected String getCategoryLabel(int position) {
                return ((BattleFormat.Category) getItem(position)).getLabel();
            }

            @Override
            protected View getItemView(int position, View convertView, ViewGroup parent) {
                TextView textView;
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
                    textView = (TextView) convertView;
                    textView.setSingleLine();
                } else {
                    textView = (TextView) convertView;
                }

                BattleFormat format = (BattleFormat) getItem(position);
                textView.setText("\t");
                textView.append(format.getLabel());
                textView.setTextColor(format.isTeamNeeded() ? Color.RED : Color.GREEN);

                return textView;
            }
        });
        mFormatsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                CategoryAdapter adapter = (CategoryAdapter) adapterView.getAdapter();
                BattleFormat format = (BattleFormat) adapter.getItem(position);
                setCurrentBattleFormat(format);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        mTeamsSpinner = view.findViewById(R.id.spinner_teams);
        mTeamsSpinner.setAdapter(new CategoryAdapter(getContext()) {
            @Override
            protected boolean isCategoryItem(int position) {
                return getItem(position) instanceof Team.Group;
            }

            @Override
            protected String getCategoryLabel(int position) {
                return ((Team.Group) getItem(position)).format;
            }

            class ViewHolder {

                TextView labelView;
                ImageView[] pokemonViews;

                ViewHolder(View view) {
                    labelView = view.findViewById(R.id.text_view_title);
                    pokemonViews = new ImageView[6];
                    pokemonViews[0] = view.findViewById(R.id.image_view_pokemon1);
                    pokemonViews[1] = view.findViewById(R.id.image_view_pokemon2);
                    pokemonViews[2] = view.findViewById(R.id.image_view_pokemon3);
                    pokemonViews[3] = view.findViewById(R.id.image_view_pokemon4);
                    pokemonViews[4] = view.findViewById(R.id.image_view_pokemon5);
                    pokemonViews[5] = view.findViewById(R.id.image_view_pokemon6);
                }
            }

            @Override
            protected View getItemView(int position, View convertView, ViewGroup parent) {
                final ViewHolder viewHolder;
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.dropdown_item_team, parent, false);
                    viewHolder = new ViewHolder(convertView);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                Team team = (Team) getItem(position);
                viewHolder.labelView.setText(team.label);
                String[] queries = new String[team.pokemons.size()];

                for (int k = 0; k < queries.length; k++) {
                    queries[k] = toId(team.pokemons.get(k).species);
                }
                mDexIconLoader.load(queries, new DataLoader.Callback<Bitmap>() {
                    @Override
                    public void onLoaded(Bitmap[] results) {
                        for (int k = 0; k < results.length; k++) {
                            Drawable icon = new BitmapDrawable(results[k]);
                            viewHolder.pokemonViews[k].setImageDrawable(icon);
                        }
                    }
                });

                for (int k = queries.length; k < viewHolder.pokemonViews.length; k++)
                    viewHolder.pokemonViews[k].setImageDrawable(null);

                return convertView;
            }
        });

        mBattleButton = view.findViewById(R.id.button_battle);
        mBattleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mObserver.isUserGuest())
                    SignInDialog.newInstance().show(getFragmentManager(), "");
                else {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity.getBattleFragment().battleRunning()) {
                        Snackbar.make(getView(), "A battle is already running", Snackbar.LENGTH_SHORT).show();
                    } else {
                        mAutoJoinNextBattle = true;
                        mObserver.searchForBattle();
                        mBattleButton.setText("Searching...");
                        mBattleButton.setEnabled(false);
                    }
                }
//                mObserver.fakeBattle();
//                ((MainActivity) getActivity()).showBattleFragmentView();
            }
        });
    }

    private void setCurrentBattleFormat(BattleFormat battleFormat) {
        mCurrentBattleFormat = battleFormat;

        CategoryAdapter adapter = (CategoryAdapter) mTeamsSpinner.getAdapter();
        adapter.clearItems();
        if (battleFormat.isTeamNeeded()) {
            List<Team.Group> teamGroups = ((MainActivity) getActivity()).getTeamsFragment().getTeamGroups();
            for (Team.Group group : teamGroups) {
                adapter.addItem(group);
                adapter.addItems(group.teams);
            }
            mTeamsSpinner.setSelection(1);
            mTeamsSpinner.setEnabled(true);
        } else {
            adapter.addItem(Team.dummyTeam("Random"));
            mTeamsSpinner.setSelection(0);
            mTeamsSpinner.setEnabled(false);
        }
    }

    private final GlobalMessageObserver mObserver = new GlobalMessageObserver() {

        @Override
        protected void onUserChanged(String userName, boolean isGuest, String avatarId) {
            if (mCurrentDialog != null) {
                mCurrentDialog.dismiss();
                mCurrentDialog = null;
            }
            mCurrentUserName = userName;
            if (!mObserver.isUserGuest())
                Snackbar.make(getView(), "Connected as " + userName, Snackbar.LENGTH_LONG).show();
        }

        @Override
        protected void onBattleFormatsChanged(final List<BattleFormat.Category> battleFormats) {
            CategoryAdapter adapter = (CategoryAdapter) mFormatsSpinner.getAdapter();
            adapter.clearItems();
            for (BattleFormat.Category category : battleFormats) {
                adapter.addItem(category);
                adapter.addItems(category.getBattleFormats());
            }
            mFormatsSpinner.setSelection(1);
        }

        @Override
        protected void onBattlesFound(final String[] battleRoomIds, String[] battleRoomNames) {
            final MainActivity activity = (MainActivity) getActivity();
            if (activity.getBattleFragment().battleRunning())
                return;


            if (true)
                return;
            // If sie > 2 show warning unsupport popup
            if (battleRoomIds.length > 1) {
                Snackbar.make(getView(),
                        "This client supports only one battle at a time",
                        Snackbar.LENGTH_INDEFINITE).setAction("Got it", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Auto dismiss
                    }
                }).show();
            }

            /*if (mAutoJoinNextBattle) {
                mAutoJoinNextBattle = false;
                activity.getBattleFragment().startBattle(battleRoomIds[0], true);
                activity.showBattleFragmentView();
                mBattleButton.setText("Battle !");
                mBattleButton.setEnabled(true);
            } else {
                new AlertDialog.Builder(getContext())
                        .setTitle("You are currently in a battle")
                        .setMessage("Do you want to join the following battle: " + battleRoomNames[0] + " ?")
                        .setPositiveButton("Join", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                activity.getBattleFragment().startBattle(battleRoomIds[0], false);
                                activity.showBattleFragmentView();
                                dialogInterface.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .show();
            }*/
        }

        @Override
        protected void onShowPopup(String message) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }

        @Override
        protected void onAvailableRoomsChanged(RoomInfo[] officialRooms, RoomInfo[] chatRooms) {
            MainActivity activity = (MainActivity) getActivity();
            activity.getChatFragment().onAvailableRoomsChanged(officialRooms, chatRooms);
        }

        @Override
        protected void onAvailableBattleRoomsChanged(AvailableBattleRoomsInfo availableRoomsInfo) {

        }

        @Override
        protected void onRoomInit(String roomId, String type) {
            MainActivity activity = (MainActivity) getActivity();
            switch (type) {
                case "battle":
                    BattleFragment battleFragment = activity.getBattleFragment();
                    if (battleFragment.getObservedRoomId() == null)
                        battleFragment.setObservedRoomId(roomId);
                    break;
                case "chat":
                    ChatFragment chatFragment = activity.getChatFragment();
                    if (chatFragment.getObservedRoomId() == null)
                        chatFragment.setObservedRoomId(roomId);
                    break;
            }
        }

        @Override
        protected void onRoomDeinit(String roomId) {
            MainActivity activity = (MainActivity) getActivity();

            BattleFragment battleFragment = activity.getBattleFragment();
            if (TextUtils.equals(battleFragment.getObservedRoomId(), roomId))
                battleFragment.setObservedRoomId(null);

            ChatFragment chatFragment = activity.getChatFragment();
            if (TextUtils.equals(chatFragment.getObservedRoomId(), roomId))
                chatFragment.setObservedRoomId(null);

        }

        @Override
        protected void onNetworkError() {
            if (mCurrentDialog != null)
                mCurrentDialog.dismiss();

            Snackbar.make(getView(), "No internet connection", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Try to reconnect", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            getService().reconnectToServer();
                        }
                    })
                    .show();
        }
    };

    @Override
    public void onShowdownServiceBound(ShowdownService service) {
        service.registerMessageObserver(mObserver, true);
    }

    @Override
    public void onShowdownServiceWillUnbound(ShowdownService service) {
        service.unregisterMessageObserver(mObserver);
    }
}
