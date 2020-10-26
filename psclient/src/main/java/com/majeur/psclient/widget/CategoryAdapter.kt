package com.majeur.psclient.widget

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

abstract class CategoryAdapter(context: Context?) : BaseAdapter() {

    private val mLayoutInflater = LayoutInflater.from(context)
    private val mSpinnerItems = mutableListOf<Any>()

    fun addItems(items: Collection<Any>) {
        mSpinnerItems.addAll(items)
        notifyDataSetChanged()
    }

    fun addItem(item: Any) {
        mSpinnerItems.add(item)
        notifyDataSetChanged()
    }

    fun clearItems() {
        mSpinnerItems.clear()
        notifyDataSetChanged()
    }

    fun findItemIndex(item: Any) = mSpinnerItems.indexOf(item)
    fun findItemIndex(predicate: (Any) -> Boolean) = mSpinnerItems.indexOfFirst(predicate)

    override fun getCount() = mSpinnerItems.size

    protected abstract fun isCategoryItem(position: Int): Boolean

    override fun isEnabled(position: Int) = !isCategoryItem(position)

    override fun areAllItemsEnabled() = false

    override fun getItem(i: Int) = mSpinnerItems[i]

    override fun getItemId(i: Int) = 0L

    override fun hasStableIds() = false

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        if (isCategoryItem(position)) {
            view = if (convertView == null || convertView.getTag(CONVERT_VIEW_TAG_KEY) != CONVERT_VIEW_TAG_VALUE_CATEGORY)
                getCategoryView(position, null, parent);
            else
                getCategoryView(position, convertView, parent);

            view.setTag(CONVERT_VIEW_TAG_KEY, CONVERT_VIEW_TAG_VALUE_CATEGORY);
            return view;
        } else {
            view = if (convertView == null || convertView.getTag(CONVERT_VIEW_TAG_KEY) != CONVERT_VIEW_TAG_VALUE_ITEM)
                getItemView(position, null, parent);
            else
                getItemView(position, convertView, parent);

            view.setTag(CONVERT_VIEW_TAG_KEY, CONVERT_VIEW_TAG_VALUE_ITEM);
            return view;
        }
    }

    protected fun getCategoryView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val textView = convertView as? TextView ?:
            (mLayoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false) as TextView).apply {
                setSingleLine()
                setTypeface(null, Typeface.BOLD)
            }
        textView.text = getCategoryLabel(position)
        return textView
    }

    protected abstract fun getCategoryLabel(position: Int): String?

    protected open fun getItemView(position: Int, convertView: View?, parent: ViewGroup): View {
        val textView = convertView as? TextView ?:
        (mLayoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false) as TextView).apply {
            setSingleLine()
        }
        textView.text = "\t${getItemLabel(position)}"
        return textView
    }

    protected open fun getItemLabel(position: Int) = getItem(position).toString()

    companion object {
        private const val CONVERT_VIEW_TAG_KEY = 0x12345678
        private val CONVERT_VIEW_TAG_VALUE_CATEGORY = Any()
        private val CONVERT_VIEW_TAG_VALUE_ITEM = Any()
    }
}