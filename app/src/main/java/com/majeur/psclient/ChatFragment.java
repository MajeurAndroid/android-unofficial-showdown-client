package com.majeur.psclient;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.model.RoomInfo;
import com.majeur.psclient.service.RoomMessageObserver;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.util.Utils;
import com.majeur.psclient.util.html.Html;

import java.util.List;

import static com.majeur.psclient.model.Id.toIdSafe;

public class ChatFragment extends Fragment implements MainActivity.Callbacks {

    private InputMethodManager mInputMethodManager;
    private ShowdownService mShowdownService;
    private GlideHelper mSpritesLoader;
    private DexIconLoader mDexIconLoader;

    private TextView mChatTextView;
    private ScrollView mChatScrollView;
    private TextView mTitleView;
    private TextView mUserCountView;
    private ImageButton mTitleButton;
    private EditText mMessageView;
    private ImageButton mSendMessageView;

    private String mObservedRoomId;

    public String getObservedRoomId() {
        return mObservedRoomId;
    }

    public void setObservedRoomId(String observedRoomId) {
        mObservedRoomId = observedRoomId;
        mObserver.setObservedRoomId(observedRoomId);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSpritesLoader = ((MainActivity) context).getGlideHelper();
        mDexIconLoader = ((MainActivity) context).getDexIconLoader();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mChatTextView = view.findViewById(R.id.text_view_chat);
        mChatTextView.animate().setDuration(200);
        mChatTextView.setMovementMethod(new LinkMovementMethod());
        mChatScrollView = view.findViewById(R.id.scroll_view_chat);
        mTitleView = view.findViewById(R.id.text_view_title);
        mUserCountView = view.findViewById(R.id.text_view_users);
        mTitleButton = view.findViewById(R.id.button_join_room);
        mMessageView = view.findViewById(R.id.edit_text_chat);
        mSendMessageView = view.findViewById(R.id.button_send);

        mTitleButton.setOnClickListener(v -> {
            if (!mShowdownService.isConnected()) return;

            if (mObserver.getRoomJoined())
                mShowdownService.sendRoomCommand(mObservedRoomId, "leave");
            else
                mShowdownService.sendGlobalCommand("cmd", "rooms");
        });

        mUserCountView.setOnClickListener(v -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(v.getContext(),
                    android.R.layout.simple_list_item_1,
                    mObserver.getUsers());
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Users")
                    .setAdapter(adapter, (dialog, pos) -> {
                        mShowdownService.sendGlobalCommand("cmd userdetails", toIdSafe(adapter.getItem(pos)));
                        dialog.dismiss();
                    })
                    .setNegativeButton("Close", null)
                    .show();
        });

        mSendMessageView.setOnClickListener(v -> sendMessageIfAny());

        mMessageView.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessageIfAny();
                return true;
            }
            return false;
        });

        setDefaultUiState();
    }

    private void setDefaultUiState() {
        mTitleView.setText("—");
        mUserCountView.setText("-\nusers");
        mChatTextView.animate().alpha(0f).withEndAction(() -> {
            mChatTextView.setText("\n\n\n\n\n\n\n\n\n\nTap the join button to join a room");
            mChatTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            mChatTextView.animate().alpha(1f).withEndAction(null).start();
        }).start();
        mMessageView.getText().clear();
        mMessageView.clearFocus();
        mInputMethodManager.hideSoftInputFromWindow(mMessageView.getWindowToken(), 0);
        mMessageView.setEnabled(false);
        mSendMessageView.setEnabled(false);
        mSendMessageView.getDrawable().setAlpha(128);
        mTitleButton.setImageResource(R.drawable.ic_enter);
        mTitleButton.requestFocus();
    }

    private void sendMessageIfAny() {
        String message = mMessageView.getText().toString();
        if (message.length() == 0 || TextUtils.isEmpty(message))
            return;
        mShowdownService.sendRoomMessage(mObservedRoomId, message);
        mMessageView.getText().clear();
        mInputMethodManager.hideSoftInputFromWindow(mMessageView.getWindowToken(), 0);
    }

    @Override
    public void onServiceBound(ShowdownService service) {
        mShowdownService = service;
        service.registerMessageObserver(mObserver);
    }

    @Override
    public void onServiceWillUnbound(ShowdownService service) {
        mShowdownService = null;
        service.unregisterMessageObserver(mObserver);
    }

    void onAvailableRoomsChanged(RoomInfo[] officialRooms, RoomInfo[] chatRooms) {
        JoinRoomDialog.newInstance(officialRooms, chatRooms).show(getFragmentManager(), "");
    }

    private void notifyNewMessageReceived() {
        MainActivity activity = (MainActivity) getActivity();
        if (getId() != activity.getSelectedFragmentId())
            activity.showBadge(getId());
    }

    private final RoomMessageObserver mObserver = new RoomMessageObserver() {

        @Override
        public void onRoomInit() {
            mMessageView.setEnabled(true);
            mMessageView.requestFocus();
            mSendMessageView.setEnabled(true);
            mSendMessageView.getDrawable().setAlpha(255);
            mTitleButton.setImageResource(R.drawable.ic_exit);
            mChatTextView.setGravity(Gravity.START);
            mChatTextView.setText("", TextView.BufferType.EDITABLE);
            postFullScroll();
        }

        @Override
        public void onPrintText(CharSequence text) {
            boolean fullScrolled = Utils.fullScrolled(mChatScrollView);
            int l = mChatTextView.length();
            if (l > 0) mChatTextView.append("\n");
            mChatTextView.append(text);
            notifyNewMessageReceived();
            if (fullScrolled) postFullScroll();
        }

        @Override
        protected void onPrintHtml(String html) {
            final Object mark = new Object();
            int l = mChatTextView.length();
            mChatTextView.append("\u200C");
            mChatTextView.getEditableText().setSpan(mark, l, l + 1, Spanned.SPAN_MARK_MARK);
            Html.fromHtml(html,
                    Html.FROM_HTML_MODE_COMPACT,
                    mSpritesLoader.getHtmlImageGetter(mDexIconLoader, mChatTextView.getWidth()),
                    spanned -> {
                        int at = mChatTextView.getEditableText().getSpanStart(mark);
                        if (at == -1) return; // Check if text has been cleared
                        boolean fullScrolled = Utils.fullScrolled(mChatScrollView);
                        mChatTextView.getEditableText()
                                .insert(at, "\n")
                                .insert(at + 1, spanned);
                        notifyNewMessageReceived();
                        if (fullScrolled) postFullScroll();
                    });
        }

        private void postFullScroll() {
            mChatScrollView.post(() -> mChatScrollView.fullScroll(View.FOCUS_DOWN));
        }

        @Override
        public void onRoomTitleChanged(String title) {
            mTitleView.setText(title);
        }

        @Override
        public void onUpdateUsers(List<String> users) {
            mUserCountView.setText(users.size() + " users");
        }

        @Override
        public void onRoomDeInit() {
            setDefaultUiState();
        }
    };
}
