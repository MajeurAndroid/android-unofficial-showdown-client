package com.majeur.psclient.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButton;
import com.majeur.psclient.R;
import com.majeur.psclient.model.BattleFormat;
import com.majeur.psclient.ui.MainActivity;

import java.util.List;

import static com.majeur.psclient.util.Utils.boldText;
import static com.majeur.psclient.util.Utils.coloredText;
import static com.majeur.psclient.util.Utils.italicText;
import static com.majeur.psclient.util.Utils.smallText;

public class PrivateMessagesOverviewWidget extends LinearLayout implements View.OnClickListener {

    private final LayoutInflater mLayoutInflater;

    private OnItemClickListener mOnItemClickListener;
    private OnItemButtonClickListener mOnItemButtonClickListener;

    public PrivateMessagesOverviewWidget(Context context) {
        this(context, null);
    }

    public PrivateMessagesOverviewWidget(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PrivateMessagesOverviewWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        mLayoutInflater = LayoutInflater.from(context);
    }

    public void incrementPmCount(String with) {
        Entry entry = getEntryOrCreate(with);
        entry.pmCount += 1;
        updateViewForEntry(entry);
    }

    public void updateChallengeTo(String to, String format) {
        Entry[] entries = getEntries();
        for (Entry entry : entries) {
            if (entry.with.equals(to)) {
                entry.challengeFormat = format;
                entry.fromMe = true;
                updateViewForEntry(entry);
            } else if (entry.fromMe) {
                entry.challengeFormat = null;
                updateViewForEntry(entry);
            }
        }
    }

    public void updateChallengesFrom(String[] usersFrom, String[] formatsFrom) {
        Entry[] entries = getEntries();
        if (usersFrom.length == 0) {
            for (Entry entry : entries) {
                if (entry.challengeFormat != null) {
                    entry.challengeFormat = null;
                    if (entry.pmCount > 0)
                        updateViewForEntry(entry);
                    else
                        removeViewForEntry(entry);
                }
            }
            return;
        }
        for (int i = 0; i < usersFrom.length; i++) {
            boolean hasEntry = false;
            for (Entry entry : entries) {
                if (entry.with.equals(usersFrom[i])) {
                    entry.challengeFormat = formatsFrom[i];
                    entry.fromMe = false;
                    updateViewForEntry(entry);
                    hasEntry = true;
                    break;
                } else if (entry.challengeFormat != null && !entry.fromMe) {
                    entry.challengeFormat = null;
                    updateViewForEntry(entry);
                    hasEntry = true;
                    break;
                }
            }
            if (!hasEntry) {
                Entry entry = getEntryOrCreate(usersFrom[i]);
                entry.challengeFormat = formatsFrom[i];
                entry.fromMe = false;
                updateViewForEntry(entry);
            }
        }
    }

    private Entry getEntryOrCreate(String with) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Entry entry = (Entry) child.getTag();
            if (entry.with.equals(with))
                return entry;
        }
        ViewGroup view = (ViewGroup) mLayoutInflater.inflate(R.layout.list_item_pmentry, this, false);
        view.setOnClickListener(this);
        view.findViewById(R.id.button_challenge).setOnClickListener(this);
        addView(view);
        Entry entry = new Entry(with);
        view.setTag(entry);
        return entry;
    }

    private void removeViewForEntry(Entry entry) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Entry e = (Entry) child.getTag();
            if (e == entry) {
                removeView(child);
                break;
            }
        }
    }

    private Entry[] getEntries() {
        Entry[] entries = new Entry[getChildCount()];
        for (int i = 0; i < getChildCount(); i++)
            entries[i] = (Entry) getChildAt(i).getTag();
        return entries;
    }

    private void updateViewForEntry(Entry entry) {
        ViewGroup view = null;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getTag().equals(entry)) view = (ViewGroup) child;
        }

        TextView textView = view.findViewById(R.id.label);
        textView.setText(boldText(entry.with));

        MaterialButton button = view.findViewById(R.id.button_challenge);
        button.setText("Challenge");
        button.setEnabled(true);

        boolean hasChallenge = entry.challengeFormat != null;
        if (hasChallenge) {
            if (entry.fromMe) {
                button.setEnabled(false);
            } else {
                button.setText("Accept");
                button.setEnabled(true);
                textView.append(smallText(coloredText(" is challenging you !", getResources().getColor(R.color.secondary))));
                textView.append("\n");
                textView.append(smallText(resolveFormat(entry.challengeFormat)));
            }
        }

        if (entry.pmCount > 0) {
            textView.append("\n");
            textView.append(smallText(italicText(entry.pmCount + " message(s)")));
        }
    }

    private String resolveFormat(String format) { // This can be done in a nicer way
        MainActivity activity = (MainActivity) getContext();
        List<BattleFormat.Category> formats = activity.getService().getSharedData("formats");
        if (formats != null) return BattleFormat.resolveName(formats, format);
        return format;
    }

    @Override
    public void onClick(View v) {
        Entry entry;
        if (v instanceof MaterialButton) { // Challenge btn
            entry = (Entry) ((View) v.getParent()).getTag();
            if (entry.challengeFormat != null && !entry.fromMe) {
                if (mOnItemButtonClickListener != null)
                    mOnItemButtonClickListener.onAcceptButtonClick(this, entry.with, entry.challengeFormat);
            } else {
                if (mOnItemButtonClickListener != null)
                    mOnItemButtonClickListener.onChallengeButtonClick(this, entry.with);
            }
        } else {
            entry = (Entry) v.getTag();
            if (mOnItemClickListener != null)
                mOnItemClickListener.onItemClick(this, entry.with);
        }
    }

    public boolean isEmpty() {
        return getChildCount() == 0;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public void setOnItemButtonClickListener(OnItemButtonClickListener listener) {
        mOnItemButtonClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(PrivateMessagesOverviewWidget p, String with);
    }

    public interface OnItemButtonClickListener {
        void onChallengeButtonClick(PrivateMessagesOverviewWidget p, String with);
        void onAcceptButtonClick(PrivateMessagesOverviewWidget p, String with, String format);
    }

    private static class Entry {

        public Entry(String with) {
            this.with = with;
        }

        String with;
        int pmCount;
        boolean fromMe;
        String challengeFormat;
    }
}
