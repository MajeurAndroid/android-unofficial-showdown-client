package com.majeur.psclient.util.html;

import android.view.View;
import androidx.annotation.NonNull;
import com.majeur.psclient.ui.ChatFragment;
import com.majeur.psclient.ui.MainActivity;

public class ChatCommandSpan extends ClickableSpan {

    private final String mCommand;

    public ChatCommandSpan(String command) {
        mCommand = command;
    }

    @Override
    public void onClick(@NonNull View view) {
        if (view.getContext() instanceof MainActivity) {
            MainActivity activity = (MainActivity) view.getContext();
            ChatFragment fragment = activity.getChatFragment();
            if (fragment.getObservedRoomId() != null)
                activity.getService().sendRoomMessage(fragment.getObservedRoomId(), mCommand);
        }
    }
}
