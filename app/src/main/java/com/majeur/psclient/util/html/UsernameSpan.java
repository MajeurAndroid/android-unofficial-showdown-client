package com.majeur.psclient.util.html;

import android.view.View;
import androidx.annotation.NonNull;
import com.majeur.psclient.MainActivity;
import com.majeur.psclient.service.ShowdownService;

public class UsernameSpan extends ClickableSpan {

    private final String mUsername;

    public UsernameSpan(String username) {
        mUsername = username;
    }

    @Override
    public void onClick(@NonNull View view) {
        if (!(view.getContext() instanceof MainActivity)) return;
        ShowdownService service = ((MainActivity) view.getContext()).getService();
        if (service == null) return;
        service.sendGlobalCommand("cmd userdetails", mUsername);
    }
}
