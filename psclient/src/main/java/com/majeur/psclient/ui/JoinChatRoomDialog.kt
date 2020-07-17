package com.majeur.psclient.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.majeur.psclient.databinding.DialogJoinRoomBinding
import com.majeur.psclient.databinding.ListFooterOtherRoomBinding
import com.majeur.psclient.databinding.ListItemRoomBinding
import com.majeur.psclient.model.ChatRoomInfo
import com.majeur.psclient.util.*
import java.util.*
import kotlin.collections.ArrayList


class JoinChatRoomDialog : BottomSheetDialogFragment() {

    private lateinit var officialRooms: List<ChatRoomInfo>
    private lateinit var chatRooms: List<ChatRoomInfo>

    private var _binding: DialogJoinRoomBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        officialRooms = requireArguments().getParcelableArrayList<ChatRoomInfo>(ARG_OFFICIAL_ROOMS)!!
        chatRooms = requireArguments().getParcelableArrayList<ChatRoomInfo>(ARG_CHAT_ROOMS)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogJoinRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.adapter = listAdapter
        binding.list.setOnTouchListener(NestedScrollLikeTouchListener())
        binding.list.onItemClickListener = AdapterView.OnItemClickListener { _, _, index, _ ->
            val roomInfo = listAdapter.getItem(index) as ChatRoomInfo
            joinRoom(roomInfo.name)
        }
        val footerBinding = ListFooterOtherRoomBinding.inflate(layoutInflater, binding.list, false)
        footerBinding.button.setOnClickListener(View.OnClickListener {
            val input = footerBinding.roomNameInput.text.toString()
            if (input.startsWith("battle-", ignoreCase = true)) {
                Toast.makeText(context, "You cannot join a battle from here", Toast.LENGTH_LONG).show()
                return@OnClickListener
            }
            joinRoom(input)
        })
        footerBinding.roomNameInput.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(editable: Editable) {
                footerBinding.button.isEnabled = editable.isNotEmpty()
            }
        })
        binding.list.addFooterView(footerBinding.root)
    }

    private fun joinRoom(roomId: String) {
        val regex = "[^a-z0-9-]".toRegex()
        roomId.toLowerCase(Locale.ROOT).replace(regex, "").let {
            (requireActivity() as MainActivity).service?.sendGlobalCommand("join", roomId)
            dismiss()
        }
    }

    private val listAdapter: ListAdapter = object : BaseAdapter() {

        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_REGULAR = 1

        override fun getCount() = 2 + officialRooms.size + chatRooms.size

        override fun isEnabled(position: Int) = getItemViewType(position) == VIEW_TYPE_REGULAR

        override fun areAllItemsEnabled() = false

        override fun getViewTypeCount() = 2

        override fun getItemViewType(position: Int): Int {
            return if (position == 0 || position == officialRooms.size + 1) VIEW_TYPE_HEADER else VIEW_TYPE_REGULAR
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            val convertView: View
            val binding: ListItemRoomBinding
            if (view == null) {
                binding = ListItemRoomBinding.inflate(layoutInflater, viewGroup, false)
                convertView = binding.root
                convertView.tag = binding
            } else {
                convertView = view
                binding = view.tag as ListItemRoomBinding
            }
            val item = getItem(i)
            binding.apply {
                if (item is String) {
                    title.text = item.relSize(1.35f)
                    description.visibility = View.GONE
                    chatImage.visibility = View.GONE
                } else {
                    val roomInfo = item as ChatRoomInfo
                    title.text = roomInfo.name.bold()
                    title.append(" (${roomInfo.userCount})".small().italic())
                    description.text = roomInfo.description
                    description.visibility = View.VISIBLE
                    chatImage.visibility = View.VISIBLE
                }
            }
            return convertView
        }

        override fun getItem(i: Int): Any {
            val officialRoomsCount = officialRooms.size
            if (i == 0) return "Official Rooms"
            if (i < officialRoomsCount + 1) return officialRooms[i - 1]
            return if (i == officialRoomsCount + 1) "Chat Rooms" else chatRooms[i - officialRoomsCount - 2]
        }

        override fun getItemId(i: Int) = 0L
    }

    class NestedScrollLikeTouchListener : View.OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            var preventParentScroll = false
            if ((view as ListView).childCount > 0) {
                val isOnTop = view.firstVisiblePosition == 0 && view.children.first().top == view.paddingTop
                val allItemsVisible = isOnTop && view.lastVisiblePosition == view.childCount
                preventParentScroll = !isOnTop && !allItemsVisible
            }
            view.parent.requestDisallowInterceptTouchEvent(preventParentScroll)
            return view.onTouchEvent(event)
        }

    }

    companion object {

        const val FRAGMENT_TAG = "join-chat-room-dialog"

        private const val ARG_OFFICIAL_ROOMS = "official-rooms"
        private const val ARG_CHAT_ROOMS = "chat-rooms"

        fun newInstance(officialRooms: List<ChatRoomInfo>, chatRooms: List<ChatRoomInfo>): JoinChatRoomDialog {
            val joinRoomDialog = JoinChatRoomDialog()
            val bundle = Bundle()
            bundle.putParcelableArrayList(ARG_OFFICIAL_ROOMS, officialRooms as? ArrayList ?: ArrayList(officialRooms))
            bundle.putParcelableArrayList(ARG_CHAT_ROOMS, chatRooms as? ArrayList ?: ArrayList(chatRooms))
            joinRoomDialog.arguments = bundle
            return joinRoomDialog
        }
    }
}
