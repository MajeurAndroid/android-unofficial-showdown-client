package com.majeur.psclient.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.majeur.psclient.R
import com.majeur.psclient.databinding.DialogSearchReplayBinding
import com.majeur.psclient.util.*
import kotlinx.coroutines.launch
import org.json.JSONObject


class SearchReplayDialog : BottomSheetDialogFragment(), AdapterView.OnItemClickListener {

    private val fragmentScope = BaseFragment.FragmentScope()

    private lateinit var inputMethodManager: InputMethodManager

    private var _binding: DialogSearchReplayBinding? = null
    private val binding get() = _binding!!
    private lateinit var footerView: View

    private var paginationAvailable = false
    private var currentPage = 0
    private var lastSearchParams = "" to ""

    private val homeFragment
        get() = parentFragment as HomeFragment

    private val bottomSheetBehavior
        get() = BottomSheetBehavior.from(dialog!!.findViewById<View>(R.id.design_bottom_sheet))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        lifecycle.addObserver(fragmentScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(fragmentScope)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(R.id.design_bottom_sheet) as FrameLayout
            val layoutParams = sheet.layoutParams ?: FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0)
            val verticalInsets = ViewCompat.getRootWindowInsets(sheet)?.stableInsets?.run { top + bottom } ?: 0
            layoutParams.height = dialog.window?.decorView?.run { height - verticalInsets }
                    ?: FrameLayout.LayoutParams.MATCH_PARENT
            sheet.layoutParams = layoutParams
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogSearchReplayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        footerView = layoutInflater.inflate(R.layout.list_footer_replays, binding.list, false)
        footerView.findViewById<Button>(R.id.more_button).setOnClickListener { loadMoreReplays() }

        binding.userInput.apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    binding.formatInput.requestFocus()
                    return@setOnEditorActionListener true
                }
                false
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED }
        }

        binding.formatInput.apply {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    binding.searchButton.callOnClick()
                    return@setOnEditorActionListener true
                }
                false
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED }
        }

        binding.searchButton.apply {
            setOnClickListener { _ ->
                if (!isEnabled) return@setOnClickListener
                isEnabled = false
                text = "Searching..."
                val username = binding.userInput.also { it.clearFocus() }.text.toString().toId()
                val format = binding.formatInput.also { it.clearFocus() }.text.toString().toId()
                searchForReplays(username, format)
                inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
            }
            isEnabled = false
        }
        binding.list.apply {
            onItemClickListener = this@SearchReplayDialog
            setOnTouchListener(NestedScrollLikeTouchListener())
        }

        searchForReplays()
    }

    private fun searchForReplays(username: String = "", format: String = "") {
        paginationAvailable = username.isNotBlank() || format.isNotBlank() // Pagination is not supported for recent replays
        lastSearchParams = username to format
        fragmentScope.launch {
            currentPage = 0
            val replayEntries = retrieveReplayList(username, format)
            binding.list.adapter = ListAdapter(replayEntries, layoutInflater)
            if (paginationAvailable && replayEntries.size == 51)
                binding.list.addFooterView(footerView)
            else
                binding.list.removeFooterView(footerView)
            binding.searchButton.apply {
                isEnabled = true
                text = "Search"
            }
        }
    }

    private fun loadMoreReplays() {
        currentPage += 1
        binding.searchButton.isEnabled = false
        footerView.isEnabled = false
        fragmentScope.launch {
            val replayEntries = retrieveReplayList(lastSearchParams.first, lastSearchParams.second, currentPage)
            getListAdapter().addItems(replayEntries)
            if (replayEntries.size != 51)
                binding.list.removeFooterView(footerView)
            footerView.isEnabled = true
            binding.searchButton.isEnabled = true
        }
    }

    private suspend fun retrieveReplayList(username: String, format: String, page: Int = 0): List<Replay> {
        val array = homeFragment.mainActivity.service?.retrieveReplayList(username, format, page) ?: return emptyList()
        return (0 until array.length()).map { i -> Replay(array.getJSONObject(i)) }
    }

    private fun getListAdapter() = when (val adapter = binding.list.adapter) {
        is WrapperListAdapter -> adapter.wrappedAdapter as ListAdapter
        else -> adapter as ListAdapter
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val replay = getListAdapter().getItem(position)
        homeFragment.startReplay(replay.id)
        dismiss()
    }

    private class ListAdapter(list: List<Replay>, private val inflater: LayoutInflater) : BaseAdapter() {

        private val items = list.toMutableList()

        fun addItems(list: List<Replay>) {
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun getCount() = items.size

        override fun getItem(position: Int) = items[position]

        override fun getItemId(position: Int) = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: inflater.inflate(android.R.layout.simple_expandable_list_item_2, parent, false)
            val replay = getItem(position)
            view.findViewById<TextView>(android.R.id.text1).text = replay.p1.bold() concat " vs. " concat replay.p2.bold()
            view.findViewById<TextView>(android.R.id.text2).text = "[${replay.format}] " concat replay.timeFromNow().italic()
            return view
        }
    }

    private class Replay(json: JSONObject) {
        val uploadTime = json.getLong("uploadtime")
        val id: String = json.getString("id")
        val format: String = json.getString("format")
        val p1: String = json.getString("p1")
        val p2: String = json.getString("p2")

        fun timeFromNow(): String {
            val dt = System.currentTimeMillis() / 1000L - uploadTime
            return when {
                dt < 60L -> "1 minute ago"
                dt < 3600L -> "${dt/60L} minute${if (dt/60L > 1) "s" else ""} ago"
                dt < 86400L -> "${dt/3600L} hour${if (dt/3600L > 1) "s" else ""} ago"
                else -> "${dt/86400L} day${if (dt/86400L > 1) "s" else ""} ago"
            }
        }
    }

    companion object {
        const val FRAGMENT_TAG = "search-replay-dialog"
    }
}