package com.majeur.psclient;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.textfield.TextInputLayout;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.util.SimpleTextWatcher;

public class SignInDialog extends DialogFragment implements View.OnClickListener, ShowdownService.AttemptSignInCallback {

    public static final String FRAGMENT_TAG = "sign-in-dialog";

    public static SignInDialog newInstance() {
        return new SignInDialog();
    }

    private ShowdownService mService;

    private EditText mUsernameEditText;
    private TextInputLayout mUsernameInputLayout;
    private EditText mPasswordEditText;
    private TextInputLayout mPasswordInputLayout;
    private Button mSignInButton;

    private boolean mHasView;
    private boolean mIsRequiringPassword;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mService = ((MainActivity) context).getService();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mHasView = true;
        return inflater.inflate(R.layout.dialog_sign_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mUsernameEditText = view.findViewById(R.id.edit_text_username);
        //mUsernameEditText.setFilters(new InputFilter[] {new UserNameFilter(), new InputFilter.LengthFilter(18)});
        mUsernameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().matches(".*[\\|,;].*"))
                    mUsernameInputLayout.setError("| , ; are not valid characters in names");
                else
                    mUsernameInputLayout.setErrorEnabled(false);

            }
        });
        mUsernameEditText.requestFocus();
        mUsernameEditText.setOnEditorActionListener(mEnterPressListener);
        mUsernameInputLayout = (TextInputLayout) mUsernameEditText.getParent().getParent(); // TextInputLayout wraps its child in a FrameLayout

        mPasswordEditText = view.findViewById(R.id.edit_text_password);
        mPasswordEditText.setOnEditorActionListener(mEnterPressListener);
        mPasswordInputLayout = (TextInputLayout) mPasswordEditText.getParent().getParent();

        mSignInButton = view.findViewById(R.id.button_signin);
        mSignInButton.setOnClickListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHasView = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mService = null;
    }

    @Override
    public void onClick(View view) {
        if (mIsRequiringPassword) {
            if (mPasswordEditText.getText().length() > 0) {
                mPasswordEditText.setEnabled(false);
                mSignInButton.setText("Loading...");
                mSignInButton.setEnabled(false);
                setCancelable(false);
                mService.attemptSignIn(mUsernameEditText.getText().toString(),
                                mPasswordEditText.getText().toString(), SignInDialog.this);

            }
        } else {
            if (mUsernameEditText.getText().length() > 0) {
                mUsernameInputLayout.setEnabled(false);
                mSignInButton.setText("Loading...");
                mSignInButton.setEnabled(false);
                setCancelable(false);
                mService.attemptSignIn(mUsernameEditText.getText().toString(), SignInDialog.this);
            }
        }
    }

    @Override
    public void onSuccess() {
        if (!mHasView) return;
        setCancelable(true);
        mUsernameInputLayout.setErrorEnabled(false);
        mPasswordInputLayout.setErrorEnabled(false);
        requireDialog().dismiss();
    }

    @Override
    public void onAuthenticationRequired() {
        if (!mHasView) return;
        setCancelable(true);
        mPasswordInputLayout.setVisibility(View.VISIBLE);
        mPasswordEditText.setEnabled(true);
        mSignInButton.setText("Sign in");
        mSignInButton.setEnabled(true);
        mIsRequiringPassword = true;
        mUsernameInputLayout.setErrorEnabled(false); // If any issue happened before
    }

    @Override
    public void onError(String reason) {
        if (!mHasView) return;
        setCancelable(true);
        if (mIsRequiringPassword) {
            mPasswordEditText.setEnabled(true);
            mSignInButton.setText("Sign in");
            mSignInButton.setEnabled(true);
            mPasswordInputLayout.setError("Wrong password, please try again.");
        } else {
            mUsernameInputLayout.setError(reason);
            mUsernameInputLayout.setEnabled(true);
            mSignInButton.setText("Sign in");
            mSignInButton.setEnabled(true);
        }
    }

    private final TextView.OnEditorActionListener mEnterPressListener = (v, actionId, event) -> {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            onClick(v);
            return true;
        }
        return false;
    };
}
