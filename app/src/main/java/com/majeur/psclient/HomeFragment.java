package com.majeur.psclient;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import static com.majeur.psclient.model.Id.toId;

public class HomeFragment extends Fragment implements MainActivity.Callbacks {

    private ShowdownService mService;
    private DexIconLoader mDexIconLoader;

    private TextView mUsernameView;
    private View mUsernameContainer;

    private View mSearchBattleContainer;
    private View mCurrentBattlesContainer;
    private ViewGroup mBattleButtonsContainer;
    private Button mBattleButton;
    private Spinner mFormatsSpinner;
    private Spinner mTeamsSpinner;
    private BattleFormat mCurrentBattleFormat;
    private List<BattleFormat.Category> mBattleFormats;

    private String mCurrentUserName;
    private String mPendingBattleToJoin;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mDexIconLoader = ((MainActivity) context).getDexIconLoader();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mSearchBattleContainer = view.findViewById(R.id.searchBattleContainer);
        mCurrentBattlesContainer = view.findViewById(R.id.currentBattleContainer);
        mUsernameView = view.findViewById(R.id.username_text);
        mUsernameContainer = view.findViewById(R.id.username_container);
        mUsernameContainer.setAlpha(0f);
        ImageButton logoutButton = view.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mService.sendGlobalCommand("logout");
                mService.forgetUserLoginInfos();
            }
        });
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
            protected String getItemLabel(int position) {
                return ((BattleFormat) getItem(position)).getLabel();
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

                if (team.pokemons.isEmpty()) {
                    for (int k = 0; k < viewHolder.pokemonViews.length; k++)
                        viewHolder.pokemonViews[k].setImageDrawable(null);
                    return convertView;
                }

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
                if (!mService.isConnected()) return;
                if (mObserver.isUserGuest())
                    SignInDialog.newInstance().show(getFragmentManager(), "");
                else {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity.getBattleFragment().battleRunning()) {
                        Snackbar.make(getView(), "A battle is already running", Snackbar.LENGTH_SHORT).show();
                    } else {
                        boolean searchRequested = searchForBattle();
                        if (searchRequested) {
                            mBattleButton.setText("Searching...");
                            mBattleButton.setEnabled(false);
                        }
                    }
                }
//                mObserver.fakeBattle();
//                ((MainActivity) getActivity()).showBattleFragmentView();
            }
        });
        mBattleButtonsContainer = view.findViewById(R.id.joinedBattleContainer);
    }

    private void setCurrentBattleFormat(BattleFormat battleFormat) {
        mCurrentBattleFormat = battleFormat;
        updateTeamSpinner();
    }

    public void updateTeamSpinner() {
        if (mCurrentBattleFormat == null) return;
        CategoryAdapter adapter = (CategoryAdapter) mTeamsSpinner.getAdapter();
        adapter.clearItems();
        if (mCurrentBattleFormat.isTeamNeeded()) {
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

    public List<BattleFormat.Category> getBattleFormats() {
        return mBattleFormats;
    }

    private boolean searchForBattle() {
        if (mService == null) return false;

        if (mCurrentBattleFormat.isTeamNeeded()) {
            Team team = (Team) mTeamsSpinner.getSelectedItem();
            if (team.isEmpty()) {
                Snackbar.make(getView(), "Your team is empty !", Snackbar.LENGTH_SHORT).show();
                return false;
            }
            mService.sendGlobalCommand("utm", team.pack());
        } else {
            mService.sendGlobalCommand("utm", "null");
        }
        mService.sendGlobalCommand("search", toId(mCurrentBattleFormat.getLabel()));
        return true;
    }

    private void tryJoinBattleRoom(final String roomId) {
        MainActivity activity = (MainActivity) getActivity();
        BattleFragment battleFragment = activity.getBattleFragment();
        if (battleFragment.getObservedRoomId() == null || !battleFragment.battleRunning()) {
            mService.sendGlobalCommand("join", roomId);
        } else {
            final String runningBattleRoomId = battleFragment.getObservedRoomId();
            if (runningBattleRoomId.equals(roomId)) {
                activity.showBattleFragmentView();
                return;
            }
            String currentBattleName = runningBattleRoomId.substring("battle-".length());
            String battleName = roomId.substring("battle-".length());
            new AlertDialog.Builder(activity)
                    .setTitle("Do you want to continue ?")
                    .setMessage("Joining battle '" + battleName
                            + "' will make you leave (and forfeit) the current battle.")
                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mPendingBattleToJoin = roomId;
                            mService.sendRoomCommand(runningBattleRoomId, "forfeit");
                            mService.sendRoomCommand(runningBattleRoomId, "leave");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private final GlobalMessageObserver mObserver = new GlobalMessageObserver() {

        @Override
        protected void onUserChanged(String userName, boolean isGuest, String avatarId) {
            mCurrentUserName = userName;
            if (isGuest) {
                Snackbar.make(getView(), "Connected as guest !", Snackbar.LENGTH_LONG).show();
                mUsernameContainer.animate().alpha(0f).translationX(mUsernameContainer.getWidth()).start();
            } else {
                Snackbar.make(getView(), "Connected as " + userName, Snackbar.LENGTH_LONG).show();
                mUsernameView.setText(userName);
                mUsernameContainer.setTranslationX(mUsernameContainer.getWidth());
                mUsernameContainer.animate().alpha(1f).translationX(0).start();
            }
        }

        @Override
        protected void onBattleFormatsChanged(List<BattleFormat.Category> battleFormats) {
            mBattleFormats = battleFormats;
            CategoryAdapter adapter = (CategoryAdapter) mFormatsSpinner.getAdapter();
            adapter.clearItems();
            for (BattleFormat.Category category : battleFormats) {
                adapter.addItem(category);
                adapter.addItems(category.getBattleFormats());
            }
            mFormatsSpinner.setSelection(1);
        }

        @Override
        protected void onSearchBattlesChanged(String[] battleRoomIds, String[] battleRoomNames) {
            mBattleButtonsContainer.removeAllViews();
            if (battleRoomIds.length == 0) {
                mCurrentBattlesContainer.setVisibility(View.GONE);
                mSearchBattleContainer.setVisibility(View.VISIBLE);
            } else {
                mCurrentBattlesContainer.setVisibility(View.VISIBLE);
                mSearchBattleContainer.setVisibility(View.GONE);
            }
            for (int i = 0; i < battleRoomIds.length; i++) {
                final String roomId = battleRoomIds[i];
                getLayoutInflater().inflate(R.layout.joined_battle_button, mBattleButtonsContainer);
                Button button = (Button) mBattleButtonsContainer
                        .getChildAt(mBattleButtonsContainer.getChildCount() - 1);
                button.setText(battleRoomNames[i]);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        tryJoinBattleRoom(roomId);
                    }
                });
            }
            mBattleButton.setText("Battle !");
            mBattleButton.setEnabled(true);
        }

        @Override
        protected void onShowPopup(String message) {
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_INDEFINITE);
            View view = snackbar.getView();
            TextView textView = view.findViewById(com.google.android.material.R.id.snackbar_text);
            textView.setMaxLines(5);
            snackbar.setAction("Ok", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Auto dismiss
                }
            });
            snackbar.show();

            if (!mBattleButton.isEnabled()) { // Possible team reject when searching for battle
                mBattleButton.setText("Battle !");
                mBattleButton.setEnabled(true);
            }
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
                    if (battleFragment.getObservedRoomId() == null || !battleFragment.battleRunning()) {
                        battleFragment.setObservedRoomId(roomId);
                        ((MainActivity) getActivity()).showBattleFragmentView();
                    } else {
                        // Most of the time this is an auto joined battle coming from a new search, let's
                        // just leave it silently. If the user wants to join it deliberately, he will
                        // be able to do that from the "you're currently in" menu.
                        mService.sendRoomCommand(roomId, "leave");
                    }
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
            if (TextUtils.equals(battleFragment.getObservedRoomId(), roomId)) {
                battleFragment.setObservedRoomId(null);
                if (mPendingBattleToJoin != null) {
                    mService.sendGlobalCommand("join", mPendingBattleToJoin);
                    mPendingBattleToJoin = null;
                }

            }

            ChatFragment chatFragment = activity.getChatFragment();
            if (TextUtils.equals(chatFragment.getObservedRoomId(), roomId))
                chatFragment.setObservedRoomId(null);
        }

        @Override
        protected void onNetworkError() {
            mUsernameContainer.animate().alpha(0f).translationX(mUsernameContainer.getWidth()).start();
            Snackbar.make(getView(), "No internet connection", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Try to reconnect", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Snackbar.make(getView(), "Reconnecting to Showdown server...",
                                    Snackbar.LENGTH_INDEFINITE).show();
                            getService().reconnectToServer();
                        }
                    })
                    .show();
        }
    };

    @Override
    public void onServiceBound(ShowdownService service) {
        mService = service;
        service.registerMessageObserver(mObserver, true);

        if (!mService.isConnected()) {
            Snackbar.make(getView(), "Connecting to Showdown server...",
                    Snackbar.LENGTH_INDEFINITE).show();
            service.connectToServer();
        }
    }

    @Override
    public void onServiceWillUnbound(ShowdownService service) {
        mService = null;
        service.unregisterMessageObserver(mObserver);
    }
}
