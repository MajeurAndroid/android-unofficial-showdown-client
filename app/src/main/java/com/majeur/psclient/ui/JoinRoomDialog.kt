package com.majeur.psclient.ui

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.majeur.psclient.R
import com.majeur.psclient.databinding.DialogJoinRoomBinding
import com.majeur.psclient.databinding.ListFooterOtherRoomBinding
import com.majeur.psclient.model.RoomInfo
import com.majeur.psclient.util.SimpleTextWatcher
import java.util.*


class JoinRoomDialog : DialogFragment() {

    private lateinit var officialRooms: List<RoomInfo>
    private lateinit var chatRooms: List<RoomInfo>

    private var _binding: DialogJoinRoomBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        officialRooms = requireArguments().getSerializable(ARG_OFFICIAL_ROOMS) as List<RoomInfo>
        chatRooms = requireArguments().getSerializable(ARG_CHAT_ROOMS) as List<RoomInfo>
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogJoinRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.adapter = listAdapter
        binding.list.onItemClickListener = AdapterView.OnItemClickListener { _, _, index, _ ->
            val roomInfo = listAdapter.getItem(index) as RoomInfo
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

        override fun getCount() =  2 + officialRooms.size + chatRooms.size

        override fun isEnabled(position: Int) = getItemViewType(position) == VIEW_TYPE_REGULAR

        override fun areAllItemsEnabled() = false

        override fun getViewTypeCount() = 2

        override fun getItemViewType(position: Int): Int {
            return if (position == 0 || position == officialRooms.size + 1) VIEW_TYPE_HEADER else VIEW_TYPE_REGULAR
        }

        inner class ViewHolder {
            var titleView: TextView? = null
            var descrView: TextView? = null
            var iconView: View? = null
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            val convertView: View
            val viewHolder: ViewHolder
            if (view == null) {
                convertView = layoutInflater.inflate(R.layout.list_item_room, viewGroup, false)
                viewHolder = ViewHolder()
                viewHolder.titleView = convertView.findViewById(R.id.title_text_view)
                viewHolder.descrView = convertView.findViewById(R.id.descr_text_view)
                viewHolder.iconView = convertView.findViewById(R.id.imageView)
                convertView.tag = viewHolder
            } else {
                convertView = view
                viewHolder = view.tag as ViewHolder
            }
            val item = getItem(i)
            if (item is String) {
                val spannableString = SpannableString(item)
                spannableString.setSpan(RelativeSizeSpan(1.35f), 0, spannableString.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                viewHolder.titleView!!.text = spannableString
                viewHolder.descrView!!.visibility = View.GONE
                viewHolder.iconView!!.visibility = View.GONE
            } else {
                val roomInfo = item as RoomInfo
                val spannableString = SpannableString(roomInfo.name + " (" +
                        roomInfo.userCount + ")")
                spannableString.setSpan(StyleSpan(Typeface.BOLD), 0,
                        roomInfo.name.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                spannableString.setSpan(StyleSpan(Typeface.ITALIC), roomInfo.name.length + 1,
                        spannableString.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                spannableString.setSpan(RelativeSizeSpan(0.8f), roomInfo.name.length + 1,
                        spannableString.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                viewHolder.titleView!!.text = spannableString
                viewHolder.descrView!!.text = roomInfo.description
                viewHolder.descrView!!.visibility = View.VISIBLE
                viewHolder.iconView!!.visibility = View.VISIBLE
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

    companion object {
        private const val ARG_OFFICIAL_ROOMS = "official-rooms"
        private const val ARG_CHAT_ROOMS = "chat-rooms"
        fun newInstance(officialRooms: Array<RoomInfo?>?, chatRooms: Array<RoomInfo?>?): JoinRoomDialog {
            val joinRoomDialog = JoinRoomDialog()
            val bundle = Bundle()
            bundle.putSerializable(ARG_OFFICIAL_ROOMS, officialRooms)
            bundle.putSerializable(ARG_CHAT_ROOMS, chatRooms)
            joinRoomDialog.arguments = bundle
            return joinRoomDialog
        }
    }
}
