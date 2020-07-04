package com.majeur.psclient.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.majeur.psclient.databinding.FragmentTeamsBinding
import com.majeur.psclient.databinding.ListCategoryTeamBinding
import com.majeur.psclient.databinding.ListItemTeamBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.io.TeamsStore
import com.majeur.psclient.model.common.BattleFormat
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.model.common.toId
import com.majeur.psclient.ui.teambuilder.TeamBuilderActivity
import com.majeur.psclient.util.SmogonTeamBuilder
import com.majeur.psclient.util.recyclerview.DividerItemDecoration
import com.majeur.psclient.util.recyclerview.ItemTouchHelperCallbacks
import com.majeur.psclient.util.recyclerview.OnItemClickListener
import com.majeur.psclient.util.toId
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.*


class TeamsFragment : BaseFragment(), OnItemClickListener {

    val teams: List<Team.Group> get() = groups

    private lateinit var teamsStore: TeamsStore
    private lateinit var assetLoader: AssetLoader
    private lateinit var listAdapter: TeamListAdapter

    private val groups = mutableListOf<Team.Group>()
    private val fallbackFormat = BattleFormat.FORMAT_OTHER

    private var _binding: FragmentTeamsBinding? = null
    private val binding get() = _binding!!

    private val clipboardManager
        get() = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val battleFormats
        get() = service?.getSharedData<List<BattleFormat.Category>>("formats")

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
            groups.addAll(storedGroups.sortedWith(Comparator<Team.Group> { g1, g2 ->
                BattleFormat.compare(battleFormats, g1.format, g2.format)
            }))
            groups.forEach { g -> g.teams.sort() }
            if (this@TeamsFragment::listAdapter.isInitialized) {
                listAdapter.notifyDataSetChanged()
            }
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
        binding.teamList.apply {
            listAdapter = TeamListAdapter(this@TeamsFragment)
            adapter = listAdapter
            addItemDecoration(object : DividerItemDecoration(view.context) {
                override fun shouldDrawDivider(parent: RecyclerView, child: View) =
                        parent.findContainingViewHolder(child) is TeamsFragment.TeamListAdapter.CategoryHolder
            })
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    binding.apply {
                        if (dy > 0) {
                            importFab.hide()
                            buildFab.hide()
                        } else {
                            importFab.show()
                            buildFab.show()
                        }
                    }
                }
            })
            ItemTouchHelper(object : ItemTouchHelperCallbacks(context, allowDeletion = true) {
                override fun onRemoveItem(position: Int) {
                    val team = listAdapter.getItem(position) as Team
                    removeTeam(team)
                    Snackbar.make(binding.root, "${team.label} removed", Snackbar.LENGTH_LONG)
                            .setAction("Undo") {
                                addOrUpdateTeam(team)
                            }.show()
                }
            }).attachToRecyclerView(this)
        }
        binding.buildFab.setOnClickListener {
            startTeamBuilderActivity()
        }
        binding.importFab.setOnClickListener {
            if (childFragmentManager.findFragmentByTag(ImportTeamDialog.FRAGMENT_TAG) == null)
                ImportTeamDialog().show(childFragmentManager, ImportTeamDialog.FRAGMENT_TAG)
        }
    }

    fun makeSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onItemClick(itemView: View, holder: RecyclerView.ViewHolder, position: Int) {
        val team = listAdapter.getItem(position) as Team
        startTeamBuilderActivity(team)
    }

    private fun startTeamBuilderActivity(team: Team? = null) {
        val intent = Intent(context, TeamBuilderActivity::class.java)
        val battleFormats = battleFormats
        if (battleFormats != null)
            intent.putExtra(TeamBuilderActivity.INTENT_EXTRA_FORMATS, battleFormats as Serializable)
        if (team != null)
            intent.putExtra(TeamBuilderActivity.INTENT_EXTRA_TEAM, team)
        startActivityForResult(intent, TeamBuilderActivity.INTENT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == TeamBuilderActivity.INTENT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val team = data.getSerializableExtra(TeamBuilderActivity.INTENT_EXTRA_TEAM) as Team
            addOrUpdateTeam(team)
        }
    }

    fun onBattleFormatsChanged() {
        groups.sortWith(Comparator<Team.Group> { g1, g2 ->
            BattleFormat.compare(battleFormats, g1.format, g2.format)
        })
        listAdapter.notifyDataSetChanged() // Update and sorts formats labels
    }

    fun onTeamsImported(teams: List<Team>) {
        for (team in teams) addOrUpdateTeam(team, persistTeams = false)
        persistUserTeams()
        makeSnackbar("Successfully imported ${teams.size} team(s)")
    }

    private fun addOrUpdateTeam(newTeam: Team, persistTeams: Boolean = true) {
        if (newTeam.format == null) newTeam.format = fallbackFormat.toId()
        var teamAdded = false
        for (group in groups) {
            val oldTeam = group.teams.firstOrNull { it.uniqueId == newTeam.uniqueId }
            if (oldTeam != null) { // Its an update
                if (oldTeam.format == newTeam.format) { // Format has not changed so we just replace item
                    val adapterPosition = listAdapter.getItemPosition(oldTeam)
                    val indexInGroup = group.teams.indexOf(oldTeam)
                    group.teams[indexInGroup] = newTeam
                    listAdapter.notifyItemChanged(adapterPosition)
                    teamAdded = true
                    if (oldTeam.label != newTeam.label) { // Label changed, move team to correct position
                        val newIndex = group.teams.sorted().indexOf(newTeam)
                        group.teams.add(newIndex, group.teams.removeAt(indexInGroup))
                        listAdapter.notifyItemMoved(adapterPosition, listAdapter.getItemPosition(newTeam))
                    }
                } else { // Format has changed so we need to remove team from its previous group
                    var adapterPosition = listAdapter.getItemPosition(oldTeam)
                    group.teams.remove(oldTeam)
                    listAdapter.notifyItemRemoved(adapterPosition)
                    if (group.teams.isEmpty()) {
                        adapterPosition = listAdapter.getItemPosition(group)
                        groups.remove(group)
                        listAdapter.notifyItemRemoved(adapterPosition)
                    }
                }
                break
            }
            if (group.format == newTeam.format) {
                val index = group.teams.plus(newTeam).sorted().indexOf(newTeam)
                group.teams.add(index, newTeam)
                val adapterPosition = listAdapter.getItemPosition(newTeam)
                listAdapter.notifyItemInserted(adapterPosition)
                teamAdded = true
            }
        }
        if (!teamAdded) { // No group matched our team format
            val newGroup = Team.Group(newTeam.format!!)
            val index = groups.plus(newGroup).sortedWith(Comparator<Team.Group> { g1, g2 ->
                BattleFormat.compare(battleFormats, g1.format, g2.format)
            }).indexOf(newGroup)
            groups.add(index, newGroup)
            var adapterPosition = listAdapter.getItemPosition(newGroup)
            listAdapter.notifyItemInserted(adapterPosition)
            newGroup.teams.add(newTeam)
            adapterPosition = listAdapter.getItemPosition(newTeam)
            listAdapter.notifyItemInserted(adapterPosition)
        }
        homeFragment.updateTeamSpinner()
        if (persistTeams) persistUserTeams()
    }

    private fun removeTeam(team: Team) {
        for (group in groups) {
            val matchingTeam = group.teams.firstOrNull { it.uniqueId == team.uniqueId } ?: continue
            var adapterPosition = listAdapter.getItemPosition(matchingTeam)
            group.teams.remove(matchingTeam)
            listAdapter.notifyItemRemoved(adapterPosition)
            if (group.teams.isEmpty()) {
                adapterPosition = listAdapter.getItemPosition(group)
                groups.remove(group)
                listAdapter.notifyItemRemoved(adapterPosition)
            }
            break
        }
        homeFragment.updateTeamSpinner()
        persistUserTeams()
    }

    private fun resolveFormatName(formatId: String): String {
        battleFormats?.let {
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

    private fun exportTeamToClipboard(team: Team) {
        fragmentScope.launch {
            val result = SmogonTeamBuilder.buildTeams(assetLoader, listOf(team))
            clipboardManager.setPrimaryClip(ClipData.newPlainText("Exported Teams", result))
            makeSnackbar("${team.label} copied to clipboard")
        }
    }

    private inner class TeamListAdapter(
            private val itemClickListener: OnItemClickListener
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val VIEW_TYPE_CATEGORY = 0
        private val VIEW_TYPE_ITEM = 1

        inner class CategoryHolder(val binding: ListCategoryTeamBinding) : RecyclerView.ViewHolder(binding.root)
        inner class ItemHolder(val binding: ListItemTeamBinding, var job: Job? = null) : RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {
            val pokemonViews = binding.run {
                listOf(imageViewPokemon1, imageViewPokemon2, imageViewPokemon3,
                        imageViewPokemon4, imageViewPokemon5, imageViewPokemon6)
            }

            init {
                binding.copyButton.setOnClickListener(this)
                binding.root.setOnClickListener(this)
                binding.root.setOnLongClickListener(this)
            }

            override fun onClick(v: View?) {
                if (v == binding.copyButton) {
                    exportTeamToClipboard(getItem(adapterPosition) as Team)
                } else {
                    itemClickListener.onItemClick(itemView, this, adapterPosition)
                }
            }

            override fun onLongClick(v: View?): Boolean {
                makeSnackbar("Swipe to the left to remove a team from the list")
                return true
            }
        }

        override fun getItemCount(): Int {
            var count = 0
            groups.forEach { g -> count += 1 + g.teams.size }
            return count
        }

        fun getItem(position: Int): Any? {
            var count = -1
            groups.forEach { g -> if (++count == position) return g else g.teams.forEach { if (++count == position) return it } }
            return null
        }

        fun getItemPosition(item: Any): Int {
            var count = -1
            groups.forEach { g -> ++count; if (g == item) return count else g.teams.forEach { ++count; if (it == item) return count } }
            return -1
        }

        override fun getItemViewType(position: Int) = if (getItem(position) is Team) VIEW_TYPE_ITEM else VIEW_TYPE_CATEGORY

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
            VIEW_TYPE_CATEGORY -> CategoryHolder(ListCategoryTeamBinding.inflate(layoutInflater, parent, false))
            else -> ItemHolder(ListItemTeamBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is CategoryHolder) {
                val group = getItem(position) as Team.Group
                holder.binding.text1.text = resolveFormatName(group.format)
            } else if (holder is ItemHolder) {
                val team = getItem(position) as Team
                holder.binding.textViewTitle.text = team.label
                holder.pokemonViews.forEach { it.setImageDrawable(null) }
                holder.job?.cancel()
                if (team.pokemons.isNotEmpty()) {
                    holder.job = fragmentScope.launch {
                        assetLoader.dexIcons(*team.pokemons.map { it.species.toId() }.toTypedArray()).forEachIndexed { index, bitmap ->
                            val drawable = BitmapDrawable(resources, bitmap)
                            holder.pokemonViews[index].setImageDrawable(drawable)
                        }
                    }
                }

            }
        }
    }


}
