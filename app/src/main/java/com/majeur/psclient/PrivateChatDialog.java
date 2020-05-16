package com.majeur.psclient;

import android.content.Context;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.majeur.psclient.model.User;
import com.majeur.psclient.util.TextTagSpan;
import com.majeur.psclient.util.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.majeur.psclient.model.Id.toId;
import static com.majeur.psclient.util.Utils.getTagColor;
import static java.lang.String.format;

public class PrivateChatDialog extends DialogFragment {

    public static final String FRAGMENT_TAG = "private-chat-dialog";

    private static final String ARG_CHAT_WITH = "chat-with";

    public static PrivateChatDialog newInstance(String with) {
        PrivateChatDialog dialog = new PrivateChatDialog();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_CHAT_WITH, with);
        dialog.setArguments(bundle);
        return dialog;
    }

    private ScrollView mScrollView;
    private TextView mChatLog;
    private EditText mMessageEditText;


    private final Map<String, Integer> mUsernameColorCache = new HashMap<>();
    private String mWith;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        MainActivity activity = (MainActivity) context;
        activity.getService();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWith = getArguments().getString(ARG_CHAT_WITH);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_private_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView) view.findViewById(R.id.title_text_view)).setText(format("Private chat: %s", mWith));

        mScrollView = view.findViewById(R.id.scroll_view_chat);
        mChatLog = view.findViewById(R.id.chat_log);
        mChatLog.setText("", TextView.BufferType.SPANNABLE);

        mMessageEditText = view.findViewById(R.id.edit_text_chat);
        mMessageEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                sendMessageIfAny();
                return true;
            }
            return false;
        });

        view.findViewById(R.id.button_send).setOnClickListener(v -> sendMessageIfAny());

        MainActivity activity = (MainActivity) getActivity();
        List<String> previousMessages = activity.getHomeFragment().getPrivateMessages(mWith);
        if (previousMessages != null) {
            for (String message : previousMessages) {
                onNewMessage(message);
            }
        }
    }

    public String getChatWith() {
        return mWith;
    }

    public void onNewMessage(String message) {
        int sepIndex = message.indexOf(':');

        if (message.substring(sepIndex+2).startsWith("/error")) {
            Spannable spannable = new SpannableString(message.substring(sepIndex+9));
            spannable.setSpan(new ForegroundColorSpan(0xFF8B0000), 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            printMessage(spannable);
            return;
        }

        String username = message.substring(0, sepIndex);
        int textColor = obtainUsernameColor(username);
        SpannableString spannable = new SpannableString(message);
        spannable.setSpan(new TextTagSpan(getTagColor(textColor), textColor), 0, sepIndex+1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        printMessage(spannable);
    }

    private void printMessage(CharSequence message) {
        if (mChatLog.length() > 0) mChatLog.append("\n");
        mChatLog.append(message);
        mScrollView.post(() -> mScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessageIfAny() {
        String message = mMessageEditText.getText().toString();
        if (message.length() == 0 || TextUtils.isEmpty(message))
            return;

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.getService().sendPrivateMessage(toId(mWith), message);
            mMessageEditText.getText().clear();
        }
        //mInputMethodManager.hideSoftInputFromWindow(mMessageView.getWindowToken(), 0);
    }

    private int obtainUsernameColor(String username) {
        Integer usernameColor = mUsernameColorCache.get(username);
        if (usernameColor == null) {
            if (User.getGroup(username) != 0) username = username.substring(1);
            usernameColor = Utils.hashColor(toId(username));
            mUsernameColorCache.put(username, usernameColor);
        }
        return usernameColor;
    }

//    private static class UserNameFilter implements InputFilter {
//        @Override
//        public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
//            if (charSequence.toString().matches("[a-zA-Z0-9 _]"))
//                return charSequence;
//            else
//                return "";
//        }
//    }
}
