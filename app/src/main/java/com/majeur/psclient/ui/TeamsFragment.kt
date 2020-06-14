package com.majeur.psclient.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.ExpandableListView.ExpandableListContextMenuInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.majeur.psclient.R
import com.majeur.psclient.databinding.FragmentTeamsBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.io.TeamsStore
import com.majeur.psclient.model.battle.BattleFormat
import com.majeur.psclient.model.battle.toId
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.ui.teambuilder.EditTeamActivity
import com.majeur.psclient.util.toId
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.*


class TeamsFragment : BaseFragment() {

    val teams: List<Team.Group> get() = groups

    private lateinit var teamsStore: TeamsStore
    private lateinit var assetLoader: AssetLoader
    private lateinit var listAdapter: TeamListAdapter

    private val groups = mutableListOf<Team.Group>()
    private val fallbackFormat = BattleFormat.FORMAT_OTHER

    private var _binding: FragmentTeamsBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        teamsStore = TeamsStore(context)
        assetLoader = mainActivity.assetLoader
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentScope.launch {
            val storedGroups = teamsStore.get()
            groups.clear()
            groups.addAll(storedGroups)
            notifyGroupChanged()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTeamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listAdapter = TeamListAdapter()
        binding.teamList.setAdapter(listAdapter)
        binding.teamList.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val team = listAdapter.getChild(groupPosition, childPosition)
            val intent = Intent(context, EditTeamActivity::class.java)
            val battleFormats = service!!.getSharedData<List<BattleFormat.Category>>("formats")!!
            intent.putExtra(EditTeamActivity.INTENT_EXTRA_FORMATS, battleFormats as Serializable)
            intent.putExtra(EditTeamActivity.INTENT_EXTRA_TEAM, team)
            startActivityForResult(intent, EditTeamActivity.INTENT_REQUEST_CODE)
            true
        }
        binding.teamList.setOnCreateContextMenuListener { contextMenu, _, contextMenuInfo ->
            val info = contextMenuInfo as ExpandableListContextMenuInfo
            val type = ExpandableListView.getPackedPositionType(info.packedPosition)
            if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
                requireActivity().menuInflater.inflate(R.menu.context_menu_team, contextMenu)
        }
        binding.importFab.setOnClickListener {
            val fm = requireFragmentManager()
            if (fm.findFragmentByTag(ImportTeamDialog.FRAGMENT_TAG) == null)
                ImportTeamDialog.newInstance(this@TeamsFragment)
                        .show(fm, ImportTeamDialog.FRAGMENT_TAG)
        }
        binding.teamList.setOnScrollListener(object : AbsListView.OnScrollListener {
            var state = AbsListView.OnScrollListener.SCROLL_STATE_IDLE
            val showFab = Runnable { binding.importFab.show() }
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                state = scrollState
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) view.postDelayed(showFab, 500)
            }

            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                if (state == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && totalItemCount > visibleItemCount
                        && firstVisibleItem + visibleItemCount < totalItemCount)
                    binding.importFab.apply {
                        if (isShown) hide() else removeCallbacks(showFab)
                    }
            }
        })
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.menuInfo !is ExpandableListContextMenuInfo) return false
        val info = item.menuInfo as ExpandableListContextMenuInfo
        val groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition)
        val childPos = ExpandableListView.getPackedPositionChild(info.packedPosition)
        val team = listAdapter.getChild(groupPos, childPos)
        return when (item.itemId) {
            R.id.action_rename -> {
                val dialogView = layoutInflater.inflate(R.layout.dialog_team_name, null)
                val editText = dialogView.findViewById<EditText>(R.id.edit_text_team_name)
                editText.setText(team.label)
                MaterialAlertDialogBuilder(requireActivity())
                        .setTitle("Rename team")
                        .setPositiveButton("Done") { _, _ ->
                            val regex = "[{}:\",|\\[\\]]".toRegex()
                            var input = editText.text.toString().replace(regex, "")
                            if (input.isBlank()) input = "Unnamed team"
                            team.label = input
                            notifyGroupChanged()
                            persistUserTeams()
                        }
                        .setNegativeButton("Cancel", null)
                        .setView(dialogView)
                        .show()
                editText.requestFocus()
                true
            }
            R.id.action_duplicate -> {
                val copy = Team(team)
                addOrUpdateTeam(copy)
                persistUserTeams()
                true
            }
            R.id.action_delete -> {
                MaterialAlertDialogBuilder(requireActivity())
                        .setTitle("Are you sure you want to delete this team ?")
                        .setMessage("This action can't be undone.")
                        .setPositiveButton("Yes") { _, _ ->
                            removeTeam(team)
                            persistUserTeams()
                        }
                        .setNegativeButton("No", null)
                        .show()
                true
            }
            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EditTeamActivity.INTENT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val team = data.getSerializableExtra(EditTeamActivity.INTENT_EXTRA_TEAM) as Team
            addOrUpdateTeam(team)
            persistUserTeams()
        }
    }

    fun onBattleFormatsChanged() {
        notifyGroupChanged() // Update order and formats labels
    }

    fun onTeamsImported(teams: List<Team>) {
        for (team in teams) addOrUpdateTeam(team)
        persistUserTeams()
    }

    private fun addOrUpdateTeam(newTeam: Team) {
        if (newTeam.format == null) newTeam.format = fallbackFormat.toId()
        for (group in groups) {
            val oldTeam = group.teams.firstOrNull { it.uniqueId == newTeam.uniqueId } ?: continue
            if (oldTeam.format == newTeam.format) { // Format has not changed so we just replace item
                group.teams[group.teams.indexOf(oldTeam)] = newTeam
            } else { // Format has changed so we need to remove old team from group and place new one in its group
                group.teams.remove(oldTeam)
                if (group.teams.isEmpty()) groups.remove(group)
                val newGroup = groups.firstOrNull { it.format == newTeam.format }
                        ?: Team.Group(newTeam.format!!).also { groups.add(it) }
                newGroup.teams.add(newTeam)
            }
            break
        }
        notifyGroupChanged()
    }

    private fun removeTeam(team: Team) {
        for (group in groups) {
            val t = group.teams.firstOrNull { it.uniqueId == team.uniqueId } ?: continue
            group.teams.remove(t)
            if (group.teams.isEmpty()) groups.remove(group)
            break
        }
        notifyGroupChanged()
    }

    private fun notifyGroupChanged() {
        groups.sortWith(object : Comparator<Team.Group> {
            override fun compare(g1: Team.Group, g2: Team.Group): Int {
                if (service != null) {
                    val formats = service!!.getSharedData<List<BattleFormat.Category>>("formats")
                    if (formats != null) return BattleFormat.compare(formats, g1.format, g2.format)
                }
                return g1.format.compareTo(g2.format)
            }
        })
        groups.forEach { it.teams.sort() }
        listAdapter.notifyDataSetChanged()
        homeFragment.updateTeamSpinner()
    }

    private fun resolveFormatName(formatId: String): String {
        service?.getSharedData<List<BattleFormat.Category>>("formats")?.let {
            return BattleFormat.resolveName(it, formatId)
        }
        return formatId
    }

    private fun persistUserTeams() {
        fragmentScope.launch {
            val success = teamsStore.store(groups)
            if (!success) Snackbar.make(binding.root, "Error when saving teams", Snackbar.LENGTH_LONG).show()
        }
    }

    private inner class TeamListAdapter : BaseExpandableListAdapter() {

        override fun getGroupCount() = groups.size

        override fun getChildrenCount(i: Int) = getGroup(i).teams.size

        override fun getGroup(i: Int) = groups[i]

        override fun getChild(i: Int, j: Int): Team = getGroup(i).teams[j]

        override fun getGroupView(i: Int, b: Boolean, view: View?, parent: ViewGroup): View {
            val convertView = view ?: layoutInflater.inflate(R.layout.list_category_team, parent, false)
            (convertView as TextView).text = resolveFormatName(getGroup(i).format)
            return convertView
        }

        internal inner class ViewHolder(view: View) {
            var job: Job? = null
            val labelView: TextView = view.findViewById(R.id.text_view_title)
            val pokemonViews = listOf(
                    R.id.image_view_pokemon1, R.id.image_view_pokemon2, R.id.image_view_pokemon3,
                    R.id.image_view_pokemon4, R.id.image_view_pokemon5, R.id.image_view_pokemon6
            ).map { view.findViewById<ImageView>(it) }
        }

        override fun getChildView(i: Int, j: Int, b: Boolean, view: View?, parent: ViewGroup): View {
            val convertView = view ?: layoutInflater.inflate(R.layout.list_item_team, parent, false).apply {
                tag = ViewHolder(this)
            }
            val viewHolder = convertView.tag as ViewHolder

            val team = getChild(i, j)
            viewHolder.labelView.text = team.label
            viewHolder.pokemonViews.forEach { it.setImageDrawable(null) }
            if (team.pokemons.isEmpty()) return convertView

            viewHolder.job?.cancel()
            viewHolder.job = fragmentScope.launch {
                assetLoader.dexIcons(*team.pokemons.map { it.species.toId() }.toTypedArray()).forEachIndexed { index, bitmap ->
                    val drawable = BitmapDrawable(convertView.resources, bitmap)
                    viewHolder.pokemonViews[index].setImageDrawable(drawable)
                }
            }
            return convertView
        }

        override fun getGroupId(i: Int) = 0L

        override fun getChildId(i: Int, i1: Int) = 0L

        override fun hasStableIds() = false

        override fun isChildSelectable(i: Int, i1: Int) = true
    }
}
