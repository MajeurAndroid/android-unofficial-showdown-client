package com.majeur.psclient;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.majeur.psclient.service.ShowdownService;

public class SignInDialog extends DialogFragment implements View.OnClickListener, ShowdownService.AttemptSignInCallback {

    public static SignInDialog newInstance() {
        return new SignInDialog();
    }

    private EditText mUsernameEditText;
    private TextView mMessageTextView;
    private EditText mPasswordEditText;
    private Button mSignInButton;

    private boolean mPromptUserPassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_sign_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mUsernameEditText = view.findViewById(R.id.edit_text_username);
        mUsernameEditText.setFilters(new InputFilter[] {new UserNameFilter(), new InputFilter.LengthFilter(18)});
        mUsernameEditText.requestFocus();
        mUsernameEditText.setOnEditorActionListener(mEnterPressListener);

        mMessageTextView = view.findViewById(R.id.text_view_message);

        mPasswordEditText = view.findViewById(R.id.edit_text_password);
        mPasswordEditText.setOnEditorActionListener(mEnterPressListener);

        mSignInButton = view.findViewById(R.id.button_signin);
        mSignInButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (mPromptUserPassword) {
            if (mPasswordEditText.getText().length() < 2)
                return;

            mPasswordEditText.setEnabled(false);
            mSignInButton.setText("Loading...");
            mSignInButton.setEnabled(false);
            ((MainActivity) getActivity()).getShowdownService()
                    .attemptSignIn(mUsernameEditText.getText().toString(),
                            mPasswordEditText.getText().toString(), SignInDialog.this);

        } else {
            if (mUsernameEditText.getText().length() < 2)
                return;

            mUsernameEditText.setEnabled(false);
            mSignInButton.setText("Loading...");
            mSignInButton.setEnabled(false);
            ((MainActivity) getActivity()).getShowdownService()
                    .attemptSignIn(mUsernameEditText.getText().toString(), SignInDialog.this);
        }
    }

    @Override
    public void onSignInAttempted(boolean success, boolean registeredUsername, String reason) {
        if (mPromptUserPassword) {
            if (success) {
                getDialog().dismiss();
            } else {
                mPasswordEditText.setText(null);
                mPasswordEditText.setEnabled(true);
                mSignInButton.setText("Sign in");
                mSignInButton.setEnabled(true);
                mMessageTextView.setText("Wrong password, please try again.");
            }
        } else {
            if (success) {
                getDialog().dismiss();
            } else {
                if (registeredUsername) {
                    mPasswordEditText.setEnabled(true);
                    mPasswordEditText.requestFocus();
                    mSignInButton.setText("Sign in");
                    mSignInButton.setEnabled(true);
                    mPromptUserPassword = true;
                } else {
                    mMessageTextView.setText(reason);
                    mUsernameEditText.setEnabled(true);
                    mSignInButton.setText("Sign in");
                    mSignInButton.setEnabled(true);
                }
            }
        }
    }

    private TextView.OnEditorActionListener mEnterPressListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onClick(null);
                return true;
            }
            return false;
        }

    };

    private static class UserNameFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
            if (charSequence.toString().matches("[a-zA-Z0-9 _]"))
                return charSequence;
            else
                return "";
        }
    }
}
