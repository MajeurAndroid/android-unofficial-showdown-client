package com.majeur.psclient.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.fragment.app.DialogFragment
import com.majeur.psclient.databinding.DialogSignInBinding
import com.majeur.psclient.service.ShowdownService
import com.majeur.psclient.service.ShowdownService.AttemptSignInCallback
import com.majeur.psclient.util.SimpleTextWatcher
import kotlinx.android.synthetic.main.dialog_sign_in.*

class SignInDialog : DialogFragment(), View.OnClickListener, AttemptSignInCallback {

    private var service: ShowdownService? = null
    private var requirePassword = false

    private var _binding: DialogSignInBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        service = (context as MainActivity).service
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            username.addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(editable: Editable) {
                    if (editable.matches(NAME_REGEX))
                        usernameContainer.error = "| , ; are not valid characters in names"
                    else usernameContainer.isErrorEnabled = false
                }
            })
            username.requestFocus()
            // username.setFilters(new InputFilter[] {new UserNameFilter(), new InputFilter.LengthFilter(18)});
            username.setOnEditorActionListener(mEnterPressListener)
            password.setOnEditorActionListener(mEnterPressListener)
            button.setOnClickListener(this@SignInDialog)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        service = null
    }

    override fun onClick(view: View) {
        if (requirePassword) {
            if (binding.password.text.isNotEmpty()) {
                binding.password.isEnabled = false
                button.text = "Loading..."
                button.isEnabled = false
                isCancelable = false
                service?.attemptSignIn(binding.username.text.toString(),
                        binding.password.text.toString(), this@SignInDialog)
            }
        } else {
            if (binding.username.text.isNotEmpty()) {
                binding.username.isEnabled = false
                binding.button.text = "Loading..."
                button.isEnabled = false
                isCancelable = false
                service!!.attemptSignIn(binding.username.text.toString(), this@SignInDialog)
            }
        }
    }

    override fun onSuccess() {
        if (_binding == null) return
        isCancelable = true
        binding.usernameContainer.isErrorEnabled = false
        binding.passwordContainer.isErrorEnabled = false
        requireDialog().dismiss()
    }

    override fun onAuthenticationRequired() {
        if (_binding == null) return
        isCancelable = true
        requirePassword = true
        binding.apply {
            passwordContainer.visibility = View.VISIBLE
            password.isEnabled = true
            button.text = "Sign in"
            button.isEnabled = true
            usernameContainer.isErrorEnabled = false // If any issue happened before
        }
    }

    override fun onError(reason: String) {
        if (_binding == null) return
        isCancelable = true
        if (requirePassword) {
            binding.password.isEnabled = true
            binding.passwordContainer.error = reason
            binding.button.text = "Sign in"
            binding.button.isEnabled = true
        } else {
            binding.usernameContainer.error = reason
            binding.usernameContainer.isEnabled = true
            binding.button.text = "Sign in"
            binding.button.isEnabled = true
        }
    }

    private val mEnterPressListener = OnEditorActionListener { v: TextView, actionId: Int, _: KeyEvent? ->
        if (actionId == EditorInfo.IME_ACTION_GO) {
            onClick(v)
            return@OnEditorActionListener true
        }
        false
    }

    companion object {
        val NAME_REGEX = "[|,;]".toRegex()
        const val FRAGMENT_TAG = "sign-in-dialog"
    }
}
