package com.majeur.psclient.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class CategoryAdapter extends BaseAdapter {

    private static final int CONVERT_VIEW_TAG_KEY = 0x12345678;
    private static final Object CONVERT_VIEW_TAG_VALUE_CATEGORY = new Object();
    private static final Object CONVERT_VIEW_TAG_VALUE_ITEM = new Object();

    private LayoutInflater mLayoutInflater;
    private List<Object> mSpinnerItems;

    public CategoryAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
        mSpinnerItems = new ArrayList<>();
    }

    public void addItems(Collection<?> items) {
        mSpinnerItems.addAll(items);
        notifyDataSetChanged();
    }

    public void addItem(Object item) {
        mSpinnerItems.add(item);
        notifyDataSetChanged();
    }

    public void clearItems() {
        mSpinnerItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mSpinnerItems.size();
    }

    protected abstract boolean isCategoryItem(int position);

    @Override
    public boolean isEnabled(int position) {
        return !isCategoryItem(position);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public Object getItem(int i) {
        return mSpinnerItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (isCategoryItem(position)) {
            if (convertView == null || convertView.getTag(CONVERT_VIEW_TAG_KEY) != CONVERT_VIEW_TAG_VALUE_CATEGORY)
                convertView = getCategoryView(position, null, parent);
            else
                convertView = getCategoryView(position, convertView, parent);

            convertView.setTag(CONVERT_VIEW_TAG_KEY, CONVERT_VIEW_TAG_VALUE_CATEGORY);
            return convertView;
        } else {
            if (convertView == null || convertView.getTag(CONVERT_VIEW_TAG_KEY) != CONVERT_VIEW_TAG_VALUE_ITEM)
                convertView = getItemView(position, null, parent);
            else
                convertView = getItemView(position, convertView, parent);

            convertView.setTag(CONVERT_VIEW_TAG_KEY, CONVERT_VIEW_TAG_VALUE_ITEM);
            return convertView;
        }
    }

    protected View getCategoryView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            textView = (TextView) convertView;
            textView.setSingleLine();
            textView.setTypeface(null, Typeface.BOLD);
        } else {
            textView = (TextView) convertView;
        }

        textView.setText(getCategoryLabel(position));

        return convertView;
    }

    protected abstract String getCategoryLabel(int position);

    protected View getItemView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            textView = (TextView) convertView;
            textView.setSingleLine();
        } else {
            textView = (TextView) convertView;
        }
        textView.setText("\t");
        textView.append(getItemLabel(position));
        return textView;
    }

    protected String getItemLabel(int position) {
        return null;
    }

}
