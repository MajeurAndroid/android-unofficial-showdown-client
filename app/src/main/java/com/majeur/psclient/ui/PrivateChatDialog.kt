package com.majeur.psclient.ui

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.majeur.psclient.databinding.DialogPrivateChatBinding
import com.majeur.psclient.util.TextTagSpan
import com.majeur.psclient.util.Utils
import com.majeur.psclient.util.toId


class PrivateChatDialog : DialogFragment() {

    private val usernameColorCache = mutableMapOf<String, Int>()

    lateinit var chatWith: String
        private set

    private var _binding: DialogPrivateChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatWith = requireArguments().getString(ARG_CHAT_WITH)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogPrivateChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            title.text = "Private chat: $chatWith"
            chatLog.setText("", TextView.BufferType.SPANNABLE)
            messageInput.setOnEditorActionListener { v: TextView?, actionId: Int, _: KeyEvent? ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    sendMessageIfAny()
                    return@setOnEditorActionListener true
                }
                false
            }
            sendButton.setOnClickListener { sendMessageIfAny() }
        }
        (activity as MainActivity).homeFragment.getPrivateMessages(chatWith)?.forEach {
            onNewMessage(it)
        }
    }

    fun onNewMessage(message: String) {
        val sepIndex = message.indexOf(':')
        if (message.substring(sepIndex + 2).startsWith("/error")) {
            val spannable: Spannable = SpannableString(message.substring(sepIndex + 9))
            spannable.setSpan(ForegroundColorSpan(0xFF8B0000.toInt()), 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            printMessage(spannable)
            return
        }
        val username = message.substring(0, sepIndex)
        val textColor = obtainUsernameColor(username)
        val spannable = SpannableString(message)
        spannable.setSpan(TextTagSpan(Utils.getTagColor(textColor), textColor), 0, sepIndex + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        printMessage(spannable)
    }

    private fun printMessage(message: CharSequence) {
        if (binding.chatLog.length() > 0) binding.chatLog.append("\n")
        binding.chatLog.append(message)
        binding.root.post { binding.chatLogContainer.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendMessageIfAny() {
        val message = binding.messageInput.text.toString()
        if (message.isNotEmpty()) {
            (activity as MainActivity).service?.sendPrivateMessage(chatWith.toId(), message)
            binding.messageInput.text.clear()
        }
    }

    private fun obtainUsernameColor(username: String): Int {
        return usernameColorCache.computeIfAbsent(username.toId()) {
            Utils.hashColor(username.toId())
        }
    }

    companion object {
        const val FRAGMENT_TAG = "private-chat-dialog"
        private const val ARG_CHAT_WITH = "chat-with"
        fun newInstance(with: String?): PrivateChatDialog {
            val dialog = PrivateChatDialog()
            val bundle = Bundle()
            bundle.putString(ARG_CHAT_WITH, with)
            dialog.arguments = bundle
            return dialog
        }
    }
}
