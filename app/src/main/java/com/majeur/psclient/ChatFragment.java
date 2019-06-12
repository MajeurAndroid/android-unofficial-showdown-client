package com.majeur.psclient;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.majeur.psclient.model.RoomInfo;
import com.majeur.psclient.service.RoomMessageHandler;
import com.majeur.psclient.service.ShowdownService;

import java.util.List;

public class ChatFragment extends Fragment implements MainActivity.Callbacks {

    private InputMethodManager mInputMethodManager;

    private TextView mChatTextView;
    private ScrollView mChatScrollView;
    private TextView mTitleView;
    private TextView mUserCountView;
    private ImageButton mTitleButton;
    private EditText mMessageView;
    private ImageButton mSendMessageView;

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
        mChatScrollView = view.findViewById(R.id.scroll_view_chat);
        mTitleView = view.findViewById(R.id.text_view_title);
        mUserCountView = view.findViewById(R.id.text_view_users);
        mTitleButton = view.findViewById(R.id.button_join_room);
        mMessageView = view.findViewById(R.id.edit_text_chat);
        mSendMessageView = view.findViewById(R.id.button_send);

        mTitleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mRoomMessageHandler.roomJoined())
                    mRoomMessageHandler.leaveRoom();
                else
                    mRoomMessageHandler.requestAvailableRoomsInfo();
            }
        });

        mSendMessageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageIfAny();
            }
        });

        mMessageView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessageIfAny();
                    return true;
                }
                return false;
            }
        });

        // Set the UI to the "no room joined" state
        mRoomMessageHandler.onRoomDeInit();
    }

    private void sendMessageIfAny() {
        String message = mMessageView.getText().toString();
        if (message.length() == 0 || TextUtils.isEmpty(message))
            return;
        mRoomMessageHandler.sendChatMessage(message);
        mMessageView.getText().clear();
        mInputMethodManager.hideSoftInputFromWindow(mMessageView.getWindowToken(), 0);
    }

    @Override
    public void onShowdownServiceBound(ShowdownService showdownService) {
        showdownService.registerMessageHandler(mRoomMessageHandler);
    }

    @Override
    public void onShowdownServiceUnBound() {
        mRoomMessageHandler.release();
    }

    void onAvailableRoomsChanged(RoomInfo[] officialRooms, RoomInfo[] chatRooms) {
        JoinRoomDialog.newInstance(officialRooms, chatRooms).show(getFragmentManager(), "");
    }

    public void joinRoom(String roomId) {
        mRoomMessageHandler.joinRoom(roomId);
    }

    private RoomMessageHandler mRoomMessageHandler = new RoomMessageHandler() {

        @Override
        public void onRoomInit() {
            mMessageView.setEnabled(true);
            mMessageView.requestFocus();
            mSendMessageView.setEnabled(true);
            mSendMessageView.getDrawable().setAlpha(255);
            mTitleButton.setImageResource(R.drawable.ic_exit);
            mChatTextView.setGravity(Gravity.START);
            mChatTextView.setText(null);
        }

        @Override
        public void onPrintText(CharSequence text) {
            if (mChatTextView.getText().length() > 0)
                mChatTextView.append("\n");
            mChatTextView.append(text);
            mChatScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mChatScrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
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
            mTitleView.setText("—");
            mUserCountView.setText("-\nusers");
            mChatTextView.animate().alpha(0f).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mChatTextView.setText("\n\n\n\n\n\n\n\n\n\nTap the join button to join a room");
                    mChatTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                    mChatTextView.animate().alpha(1f).withEndAction(null).start();
                }
            }).start();
            mMessageView.getText().clear();
            mMessageView.clearFocus();
            mInputMethodManager.hideSoftInputFromWindow(mMessageView.getWindowToken(), 0);
            mMessageView.setEnabled(false);
            mSendMessageView.setEnabled(false);
            mSendMessageView.getDrawable().setAlpha(128);
            mTitleButton.setImageResource(R.drawable.ic_join);
            mTitleButton.requestFocus();
        }
    };
}
