package com.majeur.psclient;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.majeur.psclient.model.RoomInfo;
import com.majeur.psclient.util.SimpleTextWatcher;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class JoinRoomDialog extends DialogFragment {

    private static final String ARG_OFFICIAL_ROOMS = "official-rooms";
    private static final String ARG_CHAT_ROOMS = "chat-rooms";

    public static JoinRoomDialog newInstance(RoomInfo[] officialRooms, RoomInfo[] chatRooms) {
        JoinRoomDialog joinRoomDialog = new JoinRoomDialog();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ARG_OFFICIAL_ROOMS, officialRooms);
        bundle.putSerializable(ARG_CHAT_ROOMS, chatRooms);
        joinRoomDialog.setArguments(bundle);
        return joinRoomDialog;
    }

    private RoomInfo[] mOfficialRooms;
    private RoomInfo[] mChatRooms;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mOfficialRooms = (RoomInfo[]) getArguments().getSerializable(ARG_OFFICIAL_ROOMS);
        Arrays.sort(mOfficialRooms, RoomInfo.COMPARATOR);

        mChatRooms = (RoomInfo[]) getArguments().getSerializable(ARG_CHAT_ROOMS);
        Arrays.sort(mChatRooms, RoomInfo.COMPARATOR);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_join_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = (ListView) view;
        listView.setAdapter(mListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RoomInfo roomInfo = (RoomInfo) mListAdapter.getItem(i);
                joinRoom(roomInfo.name);

            }
        });
        View footerView = getLayoutInflater().inflate(R.layout.list_footer_other_room, listView, false);
        listView.addFooterView(footerView);
        final View joinButton = footerView.findViewById(R.id.button_join_room);
        final EditText editText = footerView.findViewById(R.id.other_room_input);
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = editText.getText().toString();
                if (input.startsWith("battle-")) {
                    Toast.makeText(getContext(), "You cannot join a battle from here", Toast.LENGTH_LONG).show();
                    return;
                }
                joinRoom(input);
            }
        });
        editText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                joinButton.setEnabled(editable.length() > 0);
            }
        });
    }

    private void joinRoom(String roomId) {
        roomId = roomId.toLowerCase().replaceAll("[^a-z0-9-]", "").trim();
        MainActivity activity = (MainActivity) getActivity();
        activity.getService().sendGlobalCommand("join", roomId);
        dismiss();
    }

    private final ListAdapter mListAdapter = new BaseAdapter() {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_REGULAR = 1;

        @Override
        public int getCount() {
            return 2 + mOfficialRooms.length + mChatRooms.length;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) == VIEW_TYPE_REGULAR;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0 || position == mOfficialRooms.length+1)
                return VIEW_TYPE_HEADER;
            return VIEW_TYPE_REGULAR;
        }

        class ViewHolder {
            TextView titleView;
            TextView descrView;
            View iconView;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.list_item_room, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.titleView = view.findViewById(R.id.title_text_view);
                viewHolder.descrView = view.findViewById(R.id.descr_text_view);
                viewHolder.iconView = view.findViewById(R.id.imageView);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            Object item = getItem(i);
            if (item instanceof String) {
                SpannableString spannableString = new SpannableString((String) item);
                spannableString.setSpan(new RelativeSizeSpan(1.35f), 0, spannableString.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                viewHolder.titleView.setText(spannableString);
                viewHolder.descrView.setVisibility(View.GONE);
                viewHolder.iconView.setVisibility(View.GONE);
            } else {
                RoomInfo roomInfo = (RoomInfo) item;
                SpannableString spannableString = new SpannableString(roomInfo.name + " (" +
                        roomInfo.userCount + ")");
                spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0,
                        roomInfo.name.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new StyleSpan(Typeface.ITALIC), roomInfo.name.length()+1,
                        spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new RelativeSizeSpan(0.8f), roomInfo.name.length()+1,
                        spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                viewHolder.titleView.setText(spannableString);
                viewHolder.descrView.setText(roomInfo.description);
                viewHolder.descrView.setVisibility(View.VISIBLE);
                viewHolder.iconView.setVisibility(View.VISIBLE);
            }

            return view;
        }

        @Override
        public Object getItem(int i) {
            int officialRoomsCount = mOfficialRooms.length;
            if (i == 0)
                return "Official Rooms";
            if (i < officialRoomsCount + 1)
                return mOfficialRooms[i-1];
            if (i == officialRoomsCount + 1)
                return "Chat Rooms";
            return mChatRooms[i - officialRoomsCount - 2];
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }
    };

}
