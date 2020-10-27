package com.majeur.psclient.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.WrapperListAdapter
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.majeur.psclient.databinding.DialogNewsBinding
import com.majeur.psclient.databinding.ListFooterReplaysBinding
import com.majeur.psclient.databinding.ListHeaderNewsBinding
import com.majeur.psclient.util.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.max

class NewsDialog : BottomSheetDialogFragment() {

    private val fragmentScope = BaseFragment.FragmentScope()

    private var _binding: DialogNewsBinding? = null
    private val binding get() = _binding!!
    private lateinit var headerViewBinding: ListHeaderNewsBinding
    private lateinit var footerViewBinding: ListFooterReplaysBinding

    private var oldestNewsId = 0
    private var newsBannerWasShown = false

    private val homeFragment
        get() = parentFragment as HomeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(fragmentScope)
        newsBannerWasShown = Preferences.getBoolPreference(requireContext(), "newsbanner", true)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(fragmentScope)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.apply {
            setOnTouchListener(NestedScrollLikeTouchListener())

            headerViewBinding = ListHeaderNewsBinding.inflate(layoutInflater, this, false)
            headerViewBinding.bannerSwitch.apply {
                isChecked = newsBannerWasShown
                setOnCheckedChangeListener { view, isChecked ->
                    Preferences.setPreference(view.context, "newsbanner", isChecked) }
            }
            addHeaderView(headerViewBinding.root, null, false)

            footerViewBinding = ListFooterReplaysBinding.inflate(layoutInflater, this, false)
            footerViewBinding.moreButton.setOnClickListener { loadMoreReplays() }
        }

        loadNews()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (newsBannerWasShown != headerViewBinding.bannerSwitch.isChecked) {
            if (newsBannerWasShown) homeFragment.hideNewsBanner()
            else homeFragment.showNewsBanner()
        }
    }

    private fun loadNews() {
        fragmentScope.launch {
            val replayEntries = retrieveNews()
            oldestNewsId = replayEntries.last().id
            binding.list.adapter = ListAdapter(replayEntries, layoutInflater)
            binding.list.addFooterView(footerViewBinding.root)
        }
    }

    private fun loadMoreReplays() {
        footerViewBinding.moreButton.apply {
            text = "Loading..."
            isEnabled = false
        }
        fragmentScope.launch {
            val news = retrieveMoreNews(oldestNewsId - 1, 3)
            oldestNewsId = news.last().id
            getListAdapter().addItems(news)
            if (oldestNewsId <= 1)
                binding.list.removeFooterView(footerViewBinding.root)
            footerViewBinding.moreButton.apply {
                text = "More"
                isEnabled = true
            }
        }
    }

    private suspend fun retrieveNews(): List<News> {
        val array = homeFragment.mainActivity.service?.retrieveLatestNews() ?: return emptyList()
        return (0 until array.length()).map { i -> News(array.getJSONObject(i)) }
    }

    private suspend fun retrieveMoreNews(firstId: Int, count: Int): List<News> {
        return (firstId downTo max(firstId - (count - 1), 1)).map { id ->
            val json = homeFragment.mainActivity.service?.retrieveNews(id) ?: return emptyList()
            News(json)
        }
    }

    private fun getListAdapter() = when (val adapter = binding.list.adapter) {
        is WrapperListAdapter -> adapter.wrappedAdapter as ListAdapter
        else -> adapter as ListAdapter
    }

    private class ListAdapter(list: List<News>, private val inflater: LayoutInflater) : BaseAdapter() {

        private val items = list.toMutableList()

        fun addItems(list: List<News>) {
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun getCount() = items.size

        override fun isEnabled(position: Int) = false

        override fun getItem(position: Int) = items[position]

        override fun getItemId(position: Int) = 0L

        @SuppressLint("ClickableViewAccessibility")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            view.isFocusable = false
            view.setOnTouchListener { v, event -> Utils.delegateTouchEventForLinkClick(v as TextView, event) }
            view.updatePadding(top = view.dp(16f))
            val news = getItem(position)
            val textView = view as TextView
            textView.linksClickable = true
            textView.text = news.title.bold().big() concat "\n\n" concat news.content concat
                    "—" concat news.author.bold().small() concat " " concat news.timeFromNow().small()
            return view
        }
    }

    class News(json: JSONObject) {
        val id = json.getInt("id")
        val title: String = json.getString("title")
        val content: Spanned = Html.fromHtml(json.getString("summaryHTML"))
        val author: String = json.getString("author")
        val date = json.getLong("date")

        fun timeFromNow(): String {
            val dt = System.currentTimeMillis() / 1000L - date
            return when {
                dt < 60L -> "1 minute ago"
                dt < 3600L -> "${dt/60L} minute${if (dt/60L > 1) "s" else ""} ago"
                dt < 86400L -> "${dt/3600L} hour${if (dt/3600L > 1) "s" else ""} ago"
                else -> "${dt/86400L} day${if (dt/86400L > 1) "s" else ""} ago"
            }
        }
    }

    companion object {
        const val FRAGMENT_TAG = "news-dialog"
    }
}