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
import com.majeur.psclient.databinding.DialogSearchBattleBinding
import com.majeur.psclient.model.BattleRoomInfo
import com.majeur.psclient.model.common.BattleFormat
import com.majeur.psclient.model.common.toId
import com.majeur.psclient.util.*
import com.majeur.psclient.widget.CategoryAdapter


class SearchBattleDialog : BottomSheetDialogFragment(), AdapterView.OnItemClickListener {

    private val fragmentScope = BaseFragment.FragmentScope()

    private lateinit var inputMethodManager: InputMethodManager

    private var _binding: DialogSearchBattleBinding? = null
    private val binding get() = _binding!!

    private val homeFragment
        get() = parentFragment as HomeFragment

    private val battleFormats
        get() = homeFragment.mainActivity.service?.getSharedData<List<BattleFormat.Category>>("formats")

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
        _binding = DialogSearchBattleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.formatsSelector.apply {
            adapter = object : CategoryAdapter(context) {
                override fun isCategoryItem(position: Int) = getItem(position) is BattleFormat.Category
                override fun getCategoryLabel(position: Int) = (getItem(position) as BattleFormat.Category).label
                override fun getItemLabel(position: Int) = (getItem(position) as BattleFormat).label
            }
        }

        binding.minEloSelector.apply {
            val eloLevels = listOf("No min. Elo", "1100", "1300", "1500", "1700", "1900")
            adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, eloLevels)
        }

        binding.userInput.apply {
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
                val battleFormat = binding.formatsSelector.selectedItem as BattleFormat?
                val format = battleFormat?.takeIf { it != BattleFormat.FORMAT_ALL }?.toId() ?: ""
                val minElo = binding.minEloSelector.selectedItem?.toString()?.toIntOrNull() ?: 0
                searchForBattles(format, username, minElo)
                inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
            }
            isEnabled = false
        }
        binding.list.apply {
            onItemClickListener = this@SearchBattleDialog
            setOnTouchListener(NestedScrollLikeTouchListener())
        }

        if (battleFormats != null) onBattleFormatsChanged()

        searchForBattles()
    }

    fun onBattleFormatsChanged() {
        val adapter = binding.formatsSelector.adapter as CategoryAdapter
        adapter.clearItems()
        adapter.addItem(BattleFormat.FORMAT_ALL)
        for (category in battleFormats.orEmpty()) {
            val formats = category.formats
            if (formats.isEmpty()) continue
            adapter.addItem(category)
            adapter.addItems(formats)
        }
        binding.formatsSelector.setSelection(0)
    }

    private fun searchForBattles(format: String = "", username: String = "", minElo: Int = 0) {
        val service = homeFragment.mainActivity.service ?: return
        val mineEloArg = if (minElo > 0) minElo.toString() else ""
        service.sendGlobalCommand("cmd roomlist", format, mineEloArg, username)
    }

    fun onSearchBattleResponse(battles: List<BattleRoomInfo>) {
        binding.list.adapter = ListAdapter(battles, layoutInflater)

        binding.searchButton.apply {
            isEnabled = true
            text = "Search"
        }
    }

    private fun getListAdapter() = when (val adapter = binding.list.adapter) {
        is WrapperListAdapter -> adapter.wrappedAdapter as ListAdapter
        else -> adapter as ListAdapter
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val battle = getListAdapter().getItem(position)
        homeFragment.joinRoom(battle.roomId)
        dismiss()
    }

    private class ListAdapter(private val items: List<BattleRoomInfo>, private val inflater: LayoutInflater) : BaseAdapter() {

        override fun getCount() = items.size

        override fun getItem(position: Int) = items[position]

        override fun getItemId(position: Int) = 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: inflater.inflate(android.R.layout.simple_expandable_list_item_2, parent, false)
            val battle = getItem(position)
            val format = battle.roomId.substringAfter("-").substringBeforeLast("-")
            view.findViewById<TextView>(android.R.id.text1).text = battle.p1.bold() concat " vs. " concat battle.p2.bold()
            view.findViewById<TextView>(android.R.id.text2).text = "[$format] " concat if (battle.minElo > 0) "(rated: ${battle.minElo})".italic() else ""
            return view
        }
    }

    companion object {
        const val FRAGMENT_TAG = "search-battle-dialog"
    }
}