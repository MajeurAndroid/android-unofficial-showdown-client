package com.majeur.psclient.util;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FilterableAdapter<T> extends BaseAdapter implements Filterable {

    private LayoutInflater mInflater;

    private final List<T> mData;
    private final List<T> mAdapterData;
    private String mCurrentConstraint;
    private final int mHighlightColor;

    public FilterableAdapter(T[] data, int highlightColor) {
        mData = Collections.synchronizedList(new ArrayList<>());
        Collections.addAll(mData, data);
        mAdapterData = Collections.synchronizedList(new ArrayList<>());
        Collections.addAll(mAdapterData, data);
        mHighlightColor = highlightColor;
    }

    public FilterableAdapter(Collection<T> data, int highlightColor) {
        mData = Collections.synchronizedList(new ArrayList<>(data));
        mAdapterData = Collections.synchronizedList(new ArrayList<>(data));
        mHighlightColor = highlightColor;
    }

    @Override
    public int getCount() {
        return mAdapterData.size();
    }

    @Override
    public T getItem(int position) {
        return mAdapterData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            if (mInflater == null) mInflater = LayoutInflater.from(parent.getContext());
            convertView = mInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        }

        TextView textView = (TextView) convertView;
        textView.setText(getItem(position).toString());
        highlightMatch(textView);

        return convertView;
    }

    public int getHighlightColor() {
        return mHighlightColor;
    }

    public String getCurrentConstraint() {
        return mCurrentConstraint;
    }

    protected String prepareConstraint(CharSequence constraint) {
        return constraint.toString();
    }

    protected void highlightMatch(TextView textView) {
        if (mCurrentConstraint == null || mCurrentConstraint.length() == 0) return;
        if (!(textView.getText() instanceof Spannable))
            textView.setText(textView.getText(), TextView.BufferType.SPANNABLE);
        Spannable spannable = (Spannable) textView.getText();
        int startIndex = spannable.toString().toLowerCase().indexOf(mCurrentConstraint.toLowerCase());
        spannable.setSpan(new BackgroundColorSpan(mHighlightColor),
                startIndex,
                startIndex + mCurrentConstraint.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    protected boolean matchConstraint(String constraint, T candidate) {
        return candidate.toString().toLowerCase().contains(constraint.toLowerCase());
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private final Filter mFilter = new Filter() {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint != null) {
                String constraintString = prepareConstraint(constraint);
                List<T> list = new LinkedList<>();
                for (T t : mData) {
                    if (matchConstraint(constraintString, t))
                        list.add(t);
                }
                Log.e("fdfd", Arrays.toString(list.toArray()));
                FilterResults filterResults = new FilterResults();
                filterResults.values = list;
                filterResults.count = list.size();
                return filterResults;
            } else {
                return new FilterResults();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mCurrentConstraint = constraint != null ? prepareConstraint(constraint) : null;
            if (results != null && results.count > 0 && results.values != null) {
                mAdapterData.clear();
                mAdapterData.addAll((List<T>) results.values);
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    };
}
