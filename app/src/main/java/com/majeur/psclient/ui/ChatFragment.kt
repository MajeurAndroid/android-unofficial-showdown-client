package com.majeur.psclient.ui

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import com.majeur.psclient.R
import com.majeur.psclient.databinding.FragmentChatBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.io.GlideHelper
import com.majeur.psclient.model.Id
import com.majeur.psclient.model.RoomInfo
import com.majeur.psclient.service.RoomMessageObserver
import com.majeur.psclient.service.ShowdownService
import com.majeur.psclient.util.Callback
import com.majeur.psclient.util.Utils
import com.majeur.psclient.util.html.Html


class ChatFragment : BaseFragment() {

    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var glideHelper: GlideHelper
    private lateinit var assetLoader: AssetLoader

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private var _observedRoomId: String? = null
    var observedRoomId: String?
        get() = _observedRoomId
        set(observedRoomId) {
            _observedRoomId = observedRoomId
            observer.observedRoomId = observedRoomId
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        glideHelper = mainActivity.glideHelper
        assetLoader = mainActivity.assetLoader
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chatLog.apply {
            movementMethod = LinkMovementMethod()
            animate().duration = 200
        }
        binding.joinButton.setOnClickListener {
            if (service?.isConnected != true) return@setOnClickListener
            if (observer.roomJoined) service?.sendRoomCommand(observedRoomId, "leave")
            else service?.sendGlobalCommand("cmd", "rooms")
        }
        binding.usersCount.setOnClickListener { v: View ->
            val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, observer.users)
            AlertDialog.Builder(requireActivity())
                    .setTitle("Users")
                    .setAdapter(adapter) { dialog: DialogInterface, pos: Int ->
                        service?.sendGlobalCommand("cmd userdetails", Id.toIdSafe(adapter.getItem(pos)))
                        dialog.dismiss()
                    }
                    .setNegativeButton("Close", null)
                    .show()
        }
        binding.sendButton.setOnClickListener { sendMessageIfAny() }
        binding.messageInput.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessageIfAny()
                return@setOnEditorActionListener true
            }
            false
        }
        setUiState(roomJoined = false)
    }

    private fun setUiState(roomJoined: Boolean) {
        if (roomJoined) {
            binding.apply {
                messageInput.isEnabled = true
                messageInput.requestFocus()
                sendButton.isEnabled = true
                sendButton.drawable.alpha = 255
                joinButton.setImageResource(R.drawable.ic_exit)
                chatLog.gravity = Gravity.START
                chatLog.setText("", TextView.BufferType.EDITABLE)
            }
        } else {
            binding.apply {
                roomTitle.text = "—"
                usersCount.text = "-\nusers"
                messageInput.text.clear()
                messageInput.clearFocus()
                messageInput.isEnabled = false
                sendButton.isEnabled = false
                sendButton.drawable.alpha = 128
                joinButton.setImageResource(R.drawable.ic_enter)
                joinButton.requestFocus() // Remove focus from message input widget
                chatLog.animate().alpha(0f).withEndAction {
                    chatLog.text = "\n\n\n\n\n\n\n\n\n\nTap the join button to join a room"
                    chatLog.gravity = Gravity.CENTER_HORIZONTAL
                    chatLog.animate().alpha(1f).withEndAction(null).start()
                }.start()
            }
            inputMethodManager.hideSoftInputFromWindow(binding.messageInput.windowToken, 0)
        }
    }

    private fun sendMessageIfAny() {
        val message = binding.messageInput.text.toString()
        if (message.isEmpty()) return
        service?.sendRoomMessage(observedRoomId, message)
        binding.messageInput.text.clear()
    }

    override fun onServiceBound(service: ShowdownService) {
        super.onServiceBound(service)
        service.registerMessageObserver(observer)
    }

    override fun onServiceWillUnbound(service: ShowdownService) {
        super.onServiceWillUnbound(service)
        service.unregisterMessageObserver(observer)
    }

    fun onAvailableRoomsChanged(officialRooms: List<RoomInfo>, chatRooms: List<RoomInfo>) {
        JoinRoomDialog.newInstance(officialRooms.toTypedArray(), chatRooms.toTypedArray())
                .show(requireFragmentManager(), "")
    }

    private fun notifyNewMessageReceived() {
        mainActivity.showBadge(id)
    }

    private fun postFullScroll() {
        binding.chatLogContainer.post { binding.chatLogContainer.fullScroll(View.FOCUS_DOWN) }
    }

    private val observer: RoomMessageObserver = object : RoomMessageObserver() {

        public override fun onRoomInit() {
            setUiState(roomJoined = true)
        }

        public override fun onRoomDeInit() {
            setUiState(roomJoined = false)
        }

        public override fun onPrintText(text: CharSequence) {
            val fullScrolled = Utils.fullScrolled(binding.chatLogContainer)
            if (binding.chatLog.length() > 0) binding.chatLog.append("\n")
            binding.chatLog.append(text)
            notifyNewMessageReceived()
            if (fullScrolled) postFullScroll()
        }

        override fun onPrintHtml(html: String) {
            val mark = Any()
            val l = binding.chatLog.length()
            binding.chatLog.append("\u200C")
            binding.chatLog.editableText.setSpan(mark, l, l + 1, Spanned.SPAN_MARK_MARK)
            Html.fromHtml(html,
                    Html.FROM_HTML_MODE_COMPACT,
                    glideHelper.getHtmlImageGetter(assetLoader, binding.chatLog.width),
                    Callback { spanned: Spanned? ->
                        val at = binding.chatLog.editableText.getSpanStart(mark)
                        if (at == -1) return@Callback // Check if text has been cleared
                        val fullScrolled = Utils.fullScrolled(binding.chatLogContainer)
                        binding.chatLog.editableText
                                .insert(at, "\n")
                                .insert(at + 1, spanned)
                        notifyNewMessageReceived()
                        if (fullScrolled) postFullScroll()
                    })
        }

        public override fun onRoomTitleChanged(title: String) {
            binding.roomTitle.text = title
        }

        public override fun onUpdateUsers(users: List<String>) {
            binding.usersCount.text = "${users.size} users"
        }
    }
}