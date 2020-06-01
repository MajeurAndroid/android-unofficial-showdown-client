package com.majeur.psclient;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.model.AvailableBattleRoomsInfo;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.model.RoomInfo;
import com.majeur.psclient.model.Team;
import com.majeur.psclient.service.GlobalMessageObserver;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.util.BackgroundBitmapDrawable;
import com.majeur.psclient.util.Preferences;
import com.majeur.psclient.widget.CategoryAdapter;
import com.majeur.psclient.widget.PrivateMessagesOverviewWidget;

import java.util.Collections;
import java.util.List;

import static androidx.core.graphics.ColorUtils.blendARGB;
import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.model.Id.toIdSafe;
import static com.majeur.psclient.util.Utils.boldText;
import static com.majeur.psclient.util.Utils.coloredText;
import static com.majeur.psclient.util.Utils.italicText;
import static com.majeur.psclient.util.Utils.smallText;
import static com.majeur.psclient.util.Utils.str;
import static com.majeur.psclient.util.Utils.truncate;
import static java.lang.String.format;

public class HomeFragment extends Fragment implements MainActivity.Callbacks, View.OnClickListener {

    private static final String URL_BUG_REPORT_GFORM = "https://docs.google.com/forms/d/e/1FAIpQLSfvaHpKtRhN-naHtmaIongBRzjU0rmPXu770tvjseWUNky48Q/viewform?usp=send_form";
    private static final String URL_SMOGON_THREAD = "https://www.smogon.com/forums/threads/02-23-alpha06-unofficial-showdown-android-client.3654298/";

    private ShowdownService mService;
    private DexIconLoader mDexIconLoader;

    private TextView mUsernameView;
    private TextView mUserCountView;
    private ImageButton mLoginButton;
    private TextView mBattleCountView;
    private MaterialCardView mSearchBattleContainer;
    private View mCurrentBattlesContainer;
    private ViewGroup mBattleButtonsContainer;
    private Button mBattleButton;
    private Button mCancelSearchButton;
    private ImageButton mSoundButton;
    private Spinner mFormatsSpinner;
    private Spinner mTeamsSpinner;
    private View mPmsOverviewContainer;
    private PrivateMessagesOverviewWidget mPmsOverviewWidget;

    private BattleFormat mCurrentBattleFormat;
    private List<BattleFormat.Category> mBattleFormats;
    private String mMyUsername;
    private String mPendingBattleToJoin;
    private boolean mSoundEnabled;
    private boolean mIsSearchingBattle;
    private boolean mIsChallengingSomeone;
    private boolean mWaitingForChallenge;
    private String mChallengeTo;
    private boolean mIsAcceptingChallenge;
    private String mIsAcceptingFrom;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mDexIconLoader = ((MainActivity) context).getDexIconLoader();
        mSoundEnabled = Preferences.getBoolPreference(getContext(), "sound");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (view.getBackground() == null)
            view.setBackground(new BackgroundBitmapDrawable(getResources(), R.drawable.client_bg));
        mUserCountView = view.findViewById(R.id.userCountTextView);
        mBattleCountView = view.findViewById(R.id.battleCountTextView);
        mUserCountView.setText(boldText("-"));
        mUserCountView.append(smallText("\nusers online"));
        mBattleCountView.setText(boldText("-"));
        mBattleCountView.append(smallText("\nactive battles"));
        mSearchBattleContainer = view.findViewById(R.id.searchBattleContainer);
        mCurrentBattlesContainer = view.findViewById(R.id.currentBattleContainer);
        mUsernameView = view.findViewById(R.id.username_text);
        mUsernameView.setText(smallText("Connected as\n"));
        mUsernameView.append(boldText("-"));
        mLoginButton = view.findViewById(R.id.logout_button);
        mLoginButton.setImageResource(R.drawable.ic_login);
        mLoginButton.setEnabled(false);
        mLoginButton.setOnClickListener(view12 -> {
            if (mService == null || !mService.isConnected()) return;
            if (mMyUsername == null || mMyUsername.toLowerCase().startsWith("guest")) {
                if (requireFragmentManager().findFragmentByTag(SignInDialog.FRAGMENT_TAG) == null)
                    SignInDialog.newInstance().show(requireFragmentManager(), SignInDialog.FRAGMENT_TAG);
            } else {
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
                if (!adapter.isEnabled(position)) { // Skip category label
                    mFormatsSpinner.setSelection(position+1);
                } else {
                    BattleFormat format = (BattleFormat) adapter.getItem(position);
                    setCurrentBattleFormat(format, true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        mTeamsSpinner = view.findViewById(R.id.spinner_teams);
        mTeamsSpinner.setAdapter(new TeamsAdapter());

        mBattleButton = view.findViewById(R.id.button_battle);
        mBattleButton.setOnClickListener(this);
        mSoundButton = view.findViewById(R.id.sound_button);
        mSoundButton.setImageResource(mSoundEnabled ? R.drawable.ic_sound_on : R.drawable.ic_sound_off);
        mSoundButton.setOnClickListener(view1 -> {
            mSoundEnabled = !mSoundEnabled;
            mSoundButton.setImageResource(mSoundEnabled ? R.drawable.ic_sound_on : R.drawable.ic_sound_off);
            Preferences.setPreference(getContext(), "sound", mSoundEnabled);
        });
        mCancelSearchButton = view.findViewById(R.id.button_cancel_search);
        mCancelSearchButton.setOnClickListener(this);
        mBattleButtonsContainer = view.findViewById(R.id.joinedBattleContainer);

        mPmsOverviewContainer = view.findViewById(R.id.pmsOverviewContainer);
        mPmsOverviewWidget = view.findViewById(R.id.pmsOverviewWidget);
        mPmsOverviewWidget.setOnItemClickListener((p, with) -> startPrivateChat(with));
        mPmsOverviewWidget.setOnItemButtonClickListener(new PrivateMessagesOverviewWidget.OnItemButtonClickListener() {
            @Override
            public void onChallengeButtonClick(PrivateMessagesOverviewWidget p, String with) {
                challengeSomeone(with);
            }

            @Override
            public void onAcceptButtonClick(PrivateMessagesOverviewWidget p, String with, String format) {
                if (mIsSearchingBattle) {
                    Snackbar.make(p, "Cannot accept challenge while searching for battle", Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (mIsChallengingSomeone) {
                    Snackbar.make(p, "You are already challenging someone", Snackbar.LENGTH_LONG).show();
                    return;
                }
                mIsAcceptingChallenge = true;
                mIsAcceptingFrom = with;
                setBattleButtonUIState(format("Accept\n%s's\nchallenge", with), true, true, true);
                showSearchableFormatsOnly(false);
                outer: for (BattleFormat.Category category : mBattleFormats) {
                    for (BattleFormat battleFormat : category.getBattleFormats()) {
                        if (toId(format).equals(battleFormat.id())) {
                            setCurrentBattleFormat(battleFormat, false);
                            break outer;
                        }
                    }
                }
                mFormatsSpinner.setEnabled(false);
                view.post(() -> ((ScrollView) view).fullScroll(View.FOCUS_UP));
            }
        });

        view.findViewById(R.id.button_finduser).setOnClickListener(this);
        view.findViewById(R.id.button_reportbug).setOnClickListener(view1 -> new AlertDialog.Builder(requireActivity())
                .setTitle("Wait a minute !")
                .setMessage("If the bug you want to report needs a detailed description to be clearly understood, please consider posting on the Smogon forum thread.\nIf not, you can continue to the form.\nThanks !")
                .setPositiveButton("Continue", (dialog, which) -> openUrl(URL_BUG_REPORT_GFORM, true))
                .setNeutralButton("Go to smogon thread", (dialog, which) -> openUrl(URL_SMOGON_THREAD, false))
                .setNegativeButton("Cancel", null)
                .show());
    }

    @Override
    public void onClick(View view) {
        if (mService == null || !mService.isConnected()) return;
        switch (view.getId()) {
            case R.id.button_battle:
                if (mObserver.isUserGuest()) {
                    if (requireFragmentManager().findFragmentByTag(SignInDialog.FRAGMENT_TAG) == null)
                        SignInDialog.newInstance().show(requireFragmentManager(), SignInDialog.FRAGMENT_TAG);
                } else {
                    MainActivity activity = (MainActivity) requireActivity();
                    if (activity.getBattleFragment().battleRunning()) {
                        Snackbar.make(view, "A battle is already running", Snackbar.LENGTH_SHORT).show();
                    } else {
                        searchForBattle();
                    }
                }
                break;
            case R.id.button_cancel_search:
                if (mIsChallengingSomeone) {
                    if (mWaitingForChallenge) {
                        mService.sendGlobalCommand("cancelchallenge", toId(mChallengeTo));
                    } else {
                        mIsChallengingSomeone = false;
                        mChallengeTo = null;
                        setBattleButtonUIState("Battle !", true, false, false);
                        showSearchableFormatsOnly(true);
                    }
                } else if (mIsAcceptingChallenge) {
                    mIsAcceptingChallenge = false;
                    mIsAcceptingFrom = null;
                    setBattleButtonUIState("Battle !", true, false, false);
                    showSearchableFormatsOnly(true);
                    mFormatsSpinner.setEnabled(true);
                } else {
                    mService.sendGlobalCommand("cancelsearch");
                }
                break;
            case R.id.button_finduser:
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_battle_message, null);
                final EditText editText = dialogView.findViewById(R.id.edit_text_team_name);
                editText.setHint("Type a username");
                new MaterialAlertDialogBuilder(view.getContext())
                        .setPositiveButton("Find", (dialogInterface, i) -> {
                            String input = editText.getText().toString();
                            String regex = "[{}:\",|\\[\\]]";
                            if (input.matches(".*" + regex + ".*")) input = input.replaceAll(regex, "");
                            if (input.length() > 0)
                                mService.sendGlobalCommand("cmd userdetails", input);
                        })
                        .setNegativeButton("Cancel", null)
                        .setView(dialogView)
                        .show();
                editText.requestFocus();
                break;
        }
    }

    private void openUrl(String url, boolean useChrome) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (useChrome) intent.setPackage("com.android.chrome"); // Try to use chrome to autoconnect to GForms
            startActivity(intent);
        } catch (ActivityNotFoundException e1) {
            try {
                startActivity( new Intent(Intent.ACTION_VIEW, Uri.parse(url))); // Fallback to default browser
            } catch (ActivityNotFoundException e2) {
                Snackbar.make(requireView(), "No web browser found.", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    public int compareBattleFormats(String f1, String f2) {
        if (f1.equals(f2)) return 0;
        if (f1.contains("other")) return 1;
        if (f2.contains("other")) return -1;
        if (mBattleFormats == null) return f1.compareTo(f2);
        int f1Index = -1, f2Index = -1;
        int index = 0;
        loop : for (BattleFormat.Category category : mBattleFormats)
            for (BattleFormat format : category.getBattleFormats()) {
                String id = format.id();
                if (id.equals(f1)) f1Index = index;
                if (id.equals(f2)) f2Index = index;
                if (f1Index >= 0 && f2Index >= 0) break loop;
                index++;
            }
        return Integer.compare(f1Index, f2Index);
    }

    public String resolveBattleFormatName(String formatId) {
        if (mBattleFormats == null) return formatId;
        if ("other".equals(formatId)) return "Other";
        for (BattleFormat.Category category : mBattleFormats)
            for (BattleFormat format : category.getBattleFormats())
                if (format.id().contains(formatId)) return format.getLabel();
        return formatId;
    }

    private void setCurrentBattleFormat(BattleFormat battleFormat, boolean fromUser) {
        mCurrentBattleFormat = battleFormat;
        if (!fromUser) {
            CategoryAdapter adapter = (CategoryAdapter) mFormatsSpinner.getAdapter();
            final int count = adapter.getCount();
            for (int position = 0; position < count; position++) {
                if (adapter.getItem(position).equals(battleFormat))
                    mFormatsSpinner.setSelection(position);
            }
        }
        updateTeamSpinner();
    }

    public void updateTeamSpinner() {
        if (mCurrentBattleFormat == null) return;
        CategoryAdapter adapter = (CategoryAdapter) mTeamsSpinner.getAdapter();
        adapter.clearItems();
        if (mCurrentBattleFormat.isTeamNeeded()) {
            List<Team.Group> teamGroups = ((MainActivity) requireActivity())
                    .getTeamsFragment().getTeamGroups();
            int matchingFormatGroupIndex = -1;
            int otherFormatGroupIndex = -1;
            for (int i = 0; i < teamGroups.size(); i++) {
                String id = toIdSafe(teamGroups.get(i).format);
                if (BattleFormat.FORMAT_OTHER.id().equals(id)) otherFormatGroupIndex = i;
                if (mCurrentBattleFormat.id().equals(id)) matchingFormatGroupIndex = i;
            }
            if (matchingFormatGroupIndex != -1) Collections.swap(teamGroups,
                    matchingFormatGroupIndex, 0);
            if (otherFormatGroupIndex != -1) Collections.swap(teamGroups, otherFormatGroupIndex,
                    matchingFormatGroupIndex == -1 ? 0 : 1);
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

    private void searchForBattle() {
        if (mService == null || !mService.isConnected()) return;
        if (mCurrentBattleFormat.isTeamNeeded()) {
            Team team = (Team) mTeamsSpinner.getSelectedItem();
            if (team == null) {
                Snackbar.make(requireView(), "You have no team !", Snackbar.LENGTH_SHORT).show();
                return;
            } else if (team.isEmpty()) {
                Snackbar.make(requireView(), "Your team is empty !", Snackbar.LENGTH_SHORT).show();
                return;
            }
            mService.sendGlobalCommand("utm", team.pack());
        } else {
            mService.sendGlobalCommand("utm", "null");
        }
        if (mIsChallengingSomeone) {
            mService.sendGlobalCommand("challenge", toId(mChallengeTo), toId(mCurrentBattleFormat.getLabel()));
            setBattleButtonUIState(format("Challenging\n%s...", mChallengeTo), false, true, true);
        } else if (mIsAcceptingChallenge) {
            mService.sendGlobalCommand("accept", mIsAcceptingFrom);
        } else {
            mService.sendGlobalCommand("search", toId(mCurrentBattleFormat.getLabel()));
        }
    }

    private void tryJoinBattleRoom(final String roomId) {
        if (mService == null || !mService.isConnected()) return;
        MainActivity activity = (MainActivity) requireActivity();
        BattleFragment battleFragment = activity.getBattleFragment();
        if (battleFragment.getObservedRoomId() == null || !battleFragment.battleRunning()) {
            mService.sendGlobalCommand("join", roomId);
        } else {
            final String runningBattleRoomId = battleFragment.getObservedRoomId();
            if (runningBattleRoomId.equals(roomId)) {
                activity.showBattleFragment();
                return;
            }
            //noinspection unused
            String currentBattleName = runningBattleRoomId.substring("battle-".length());
            String battleName = roomId.substring("battle-".length());
            new AlertDialog.Builder(activity)
                    .setTitle("Do you want to continue ?")
                    .setMessage(format("Joining battle '%s' will make you leave (and forfeit) the current battle.", battleName))
                    .setPositiveButton("Continue", (dialogInterface, i) -> {
                        mPendingBattleToJoin = roomId;
                        mService.sendRoomCommand(runningBattleRoomId, "forfeit");
                        mService.sendRoomCommand(runningBattleRoomId, "leave");
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    public void startPrivateChat(String user) {
        if (user.equals(mMyUsername)) {
            Snackbar.make(requireView(), "Cannot talk to yourself", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (requireFragmentManager().findFragmentByTag(PrivateChatDialog.FRAGMENT_TAG) != null) return;
        PrivateChatDialog dialog = PrivateChatDialog.newInstance(user);
        dialog.show(requireFragmentManager(), PrivateChatDialog.FRAGMENT_TAG);
    }

    public List<String> getPrivateMessages(String with) {
        return mObserver.getPrivateMessages(with);
    }

    public void challengeSomeone(String user) {
        if (user.equals(mMyUsername)) {
            Snackbar.make(requireView(), "You should try challenging yourself in an other way", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (mIsChallengingSomeone) {
            Snackbar.make(requireView(), "You are already challenging someone", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (mIsSearchingBattle) {
            Snackbar.make(requireView(), "Cannot challenge someone while searching for battle", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (mIsAcceptingChallenge) {
            Snackbar.make(requireView(), "You are already accepting a challenge", Snackbar.LENGTH_LONG).show();
            return;
        }
        if (((MainActivity) requireActivity()).getBattleFragment().battleRunning()) {
            Snackbar.make(requireView(), "You cannot challenge someone while being in a battle", Snackbar.LENGTH_LONG).show();
            return;
        }
        mIsChallengingSomeone = true;
        mWaitingForChallenge = false;
        mChallengeTo = user;
        setBattleButtonUIState(format("Challenge\n%s!", user), true, true, true);
        showSearchableFormatsOnly(false);
        MainActivity activity = (MainActivity) requireActivity();
        activity.showHomeFragment();
        requireView().post(() -> ((ScrollView) requireView()).fullScroll(View.FOCUS_UP));
    }

    private void setBattleButtonUIState(String label, boolean enabled, boolean showCancel, boolean tintCard) {
        mBattleButton.setText(label);
        mBattleButton.setEnabled(enabled);
        mTeamsSpinner.setEnabled(enabled);
        mFormatsSpinner.setEnabled(enabled);
        boolean isCancelShown = mCancelSearchButton.isShown();
        if (isCancelShown != showCancel) {
            if (showCancel) {
                mCancelSearchButton.setVisibility(View.VISIBLE);
                mCancelSearchButton.setAlpha(0f);
                mCancelSearchButton.animate().alpha(1f).setDuration(250).withEndAction(null).start();
            } else {
                mCancelSearchButton.animate().alpha(0f).setDuration(250).withEndAction(() -> mCancelSearchButton.setVisibility(View.GONE)).start();
            }
        }
        if (tintCard) mSearchBattleContainer.setCardBackgroundColor(
                blendARGB(ContextCompat.getColor(requireActivity(), R.color.surfaceBackground),
                ContextCompat.getColor(requireActivity(), R.color.primary), 0.25f));
        else mSearchBattleContainer.setCardBackgroundColor(
                ContextCompat.getColor(requireActivity(), R.color.surfaceBackground));
    }

    private void showSearchableFormatsOnly(boolean yes) {
        if (mBattleFormats == null) return;
        CategoryAdapter adapter = (CategoryAdapter) mFormatsSpinner.getAdapter();
        adapter.clearItems();
        for (BattleFormat.Category category : mBattleFormats) {
            List<BattleFormat> formats = yes ? category.getSearchableBattleFormats() : category.getBattleFormats();
            if (formats.size() == 0) continue;
            adapter.addItem(category);
            adapter.addItems(formats);
        }
        mFormatsSpinner.setSelection(1);
    }

    private void notifyNewMessageReceived() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null && getId() != activity.getSelectedFragmentId())
            activity.showBadge(getId());
    }

    @Override
    public void onServiceBound(ShowdownService service) {
        mService = service;
        service.registerMessageObserver(mObserver, true);
        if (!service.isConnected()) {
            Snackbar.make(requireView(), "Connecting to Showdown server...",
                    Snackbar.LENGTH_INDEFINITE).show();
            service.connectToServer();
        }
    }

    @Override
    public void onServiceWillUnbound(ShowdownService service) {
        mService = null;
        service.unregisterMessageObserver(mObserver);
    }

    private final GlobalMessageObserver mObserver = new GlobalMessageObserver() {

        @Override
        protected void onUserChanged(String userName, boolean isGuest, String avatarId) {
            mMyUsername = userName;
            mUsernameView.setText(smallText("Connected as\n"));
            mUsernameView.append(boldText(truncate(userName, 10)));
            mLoginButton.setEnabled(true);
            if (isGuest) {
                Snackbar.make(requireView(), "Connected as guest !", Snackbar.LENGTH_LONG).show();
                mLoginButton.setImageResource(R.drawable.ic_login);
            } else {
                Snackbar.make(requireView(), format("Connected as %s", userName), Snackbar.LENGTH_LONG).show();
                mLoginButton.setImageResource(R.drawable.ic_logout);
            }
            MainActivity activity = (MainActivity) requireActivity();
            activity.showHomeFragment();

            SignInDialog signInDialog;
            if ((signInDialog = (SignInDialog) requireFragmentManager().findFragmentByTag(SignInDialog.FRAGMENT_TAG)) != null)
                signInDialog.dismissAllowingStateLoss();

            checkRooms();
        }

        private void checkRooms() {
            MainActivity activity = (MainActivity) requireActivity();
            BattleFragment battleFragment = activity.getBattleFragment();
            if (battleFragment.getObservedRoomId() != null)
                battleFragment.setObservedRoomId(null);
            ChatFragment chatFragment = activity.getChatFragment();
            if (chatFragment.getObservedRoomId() != null)
                chatFragment.setObservedRoomId(null);
        }

        @Override
        protected void onUpdateCounts(int userCount, int battleCount) {
            mUserCountView.setText(boldText(str(userCount)));
            mUserCountView.append(smallText("\nusers online"));
            mBattleCountView.setText(boldText(str(battleCount)));
            mBattleCountView.append(smallText("\nactive battles"));
        }

        @Override
        protected void onBattleFormatsChanged(List<BattleFormat.Category> battleFormats) {
            mBattleFormats = battleFormats;
            showSearchableFormatsOnly(true);
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) activity.getTeamsFragment().onBattleFormatsChanged();
        }

        @Override
        protected void onSearchBattlesChanged(String[] searching, String[] battleRoomIds, String[] battleRoomNames) {
            mIsSearchingBattle = searching.length > 0;
            if (mIsSearchingBattle)
                setBattleButtonUIState("Searching...", false, true, false);
            else if (!mIsChallengingSomeone)
                setBattleButtonUIState("Battle !", true, false, false);

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
                getLayoutInflater().inflate(R.layout.button_joined_battle, mBattleButtonsContainer);
                Button button = (Button) mBattleButtonsContainer
                        .getChildAt(mBattleButtonsContainer.getChildCount() - 1);
                button.setText(battleRoomNames[i]);
                button.setOnClickListener(view -> tryJoinBattleRoom(roomId));
            }
        }

        @Override
        protected void onUserDetails(String id, String name, boolean online, String group,
                                     List<String> chatRooms, List<String> battles) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            if (group != null) builder.append(italicText("Group: ")).append(group.replace(" ", "␣")).append("\n");
            builder.append(italicText("Battles: "));
            if (battles.size() > 0) {
                StringBuilder stringBuilder = new StringBuilder();
                for (String battle : battles) stringBuilder.append(battle).append(", ");
                stringBuilder.deleteCharAt(stringBuilder.length() - 2);
                builder.append(smallText(stringBuilder.toString()));
            } else {
                builder.append(smallText("None")).append("\n");
            }
            builder.append(italicText("Chatrooms: "));
            if (chatRooms.size() > 0) {
                StringBuilder stringBuilder = new StringBuilder();
                for (String room : chatRooms) stringBuilder.append(room).append(", ");
                stringBuilder.deleteCharAt(stringBuilder.length() - 2);
                builder.append(smallText(stringBuilder.toString()));
            } else {
                builder.append(smallText("None")).append("\n");
            }
            if (!online) builder.append(coloredText("(Offline)", Color.RED));
            new AlertDialog.Builder(requireActivity())
                    .setTitle(name)
                    .setMessage(builder)
                    .setPositiveButton("Challenge", (dialog, which) -> challengeSomeone(name))
                    .setNegativeButton("Chat", ((dialog, which) -> startPrivateChat(name)))
                    .show();
        }

        @Override
        protected void onShowPopup(String message) {
            Snackbar snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_INDEFINITE);
            View view = snackbar.getView();
            TextView textView = view.findViewById(com.google.android.material.R.id.snackbar_text);
            textView.setMaxLines(5);
            snackbar.setAction("Ok", v -> {});
            snackbar.show();

            if (mIsChallengingSomeone) { // Resetting pending challenges
                mWaitingForChallenge = mIsChallengingSomeone = false;
                mChallengeTo = null;
                setBattleButtonUIState("Battle !", true, false, false);
                showSearchableFormatsOnly(true);
            } else if (mIsAcceptingChallenge) {
                mIsAcceptingChallenge = false;
                mIsAcceptingFrom = null;
                setBattleButtonUIState("Battle !", true, false, false);
                showSearchableFormatsOnly(true);
                mFormatsSpinner.setEnabled(true);
            }
        }

        @Override
        protected void onAvailableRoomsChanged(RoomInfo[] officialRooms, RoomInfo[] chatRooms) {
            MainActivity activity = (MainActivity) requireActivity();
            activity.getChatFragment().onAvailableRoomsChanged(officialRooms, chatRooms);
        }

        @Override
        protected void onAvailableBattleRoomsChanged(AvailableBattleRoomsInfo availableRoomsInfo) {

        }

        @Override
        protected void onNewPrivateMessage(String with, String message) {
            mPmsOverviewWidget.incrementPmCount(with);
            if (!mPmsOverviewWidget.isEmpty())
                mPmsOverviewContainer.setVisibility(View.VISIBLE);
            PrivateChatDialog dialog = (PrivateChatDialog) requireFragmentManager().findFragmentByTag(PrivateChatDialog.FRAGMENT_TAG);
            if (dialog != null && dialog.getChatWith().equals(with))
                dialog.onNewMessage(message);
            else
                notifyNewMessageReceived();
        }

        @Override
        protected void onChallengesChange(String to, String format, String[] usersFrom, String[] formatsFrom) {
            mPmsOverviewWidget.updateChallengeTo(to, format);
            mPmsOverviewWidget.updateChallengesFrom(usersFrom, formatsFrom);

            if (mPmsOverviewWidget.isEmpty()) {
                mPmsOverviewContainer.setVisibility(View.GONE);
            } else {
                mPmsOverviewContainer.setVisibility(View.VISIBLE);
                notifyNewMessageReceived();
            }

            if (mIsAcceptingChallenge) {
                boolean done = true;
                for (String user : usersFrom)
                    if (mIsAcceptingFrom.equals(user)) {
                        done = false;
                        break;
                    }
                if (done) {
                    mIsAcceptingChallenge = false;
                    mIsAcceptingFrom = null;
                    setBattleButtonUIState("Battle !", true, false, false);
                    showSearchableFormatsOnly(true);
                    mFormatsSpinner.setEnabled(true);
                }
            }

            if (mIsChallengingSomeone) {
                if (to != null) {
                    mWaitingForChallenge = true;
                    setBattleButtonUIState(format("Waiting for\n%s...", to), false, true, true);
                } else {
                    mWaitingForChallenge = mIsChallengingSomeone = false;
                    mChallengeTo = null;
                    setBattleButtonUIState("Battle !", true, false, false);
                    showSearchableFormatsOnly(true);
                }
            }
        }

        @Override
        protected void onRoomInit(String roomId, String type) {
            MainActivity activity = (MainActivity) requireActivity();
            switch (type) {
                case "battle":
                    BattleFragment battleFragment = activity.getBattleFragment();
                    if (battleFragment.getObservedRoomId() == null || !battleFragment.battleRunning()) {
                        battleFragment.setObservedRoomId(roomId);
                        activity.showBattleFragment();
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
            MainActivity activity = (MainActivity) requireActivity();

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
            Snackbar.make(requireView(), "Unable to reach Showdown server", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry", view -> {
                        Snackbar.make(requireView(), "Reconnecting to Showdown server...",
                                Snackbar.LENGTH_INDEFINITE).show();
                        getService().reconnectToServer();
                    })
                    .show();
        }
    };

    private class TeamsAdapter extends CategoryAdapter {

        public TeamsAdapter() {
            super(getContext());
        }

        @Override
        protected boolean isCategoryItem(int position) {
            return getItem(position) instanceof Team.Group;
        }

        @Override
        protected String getCategoryLabel(int position) {
            String formatId = ((Team.Group) getItem(position)).format;
            return resolveBattleFormatName(formatId);
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
            for (int k = 0; k < queries.length; k++)
                queries[k] = toId(team.pokemons.get(k).species);
            mDexIconLoader.load(queries, results -> {
                for (int k = 0; k < results.length; k++) {
                    Drawable icon = new BitmapDrawable(getResources(), results[k]);
                    viewHolder.pokemonViews[k].setImageDrawable(icon);
                }
            });

            for (int k = queries.length; k < viewHolder.pokemonViews.length; k++)
                viewHolder.pokemonViews[k].setImageDrawable(null);

            return convertView;
        }
    }
}
