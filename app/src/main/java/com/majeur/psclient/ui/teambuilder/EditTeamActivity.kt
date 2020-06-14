package com.majeur.psclient.ui.teambuilder

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.majeur.psclient.R
import com.majeur.psclient.databinding.ActivityEditTeamBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.io.GlideHelper
import com.majeur.psclient.model.common.BattleFormat
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.model.common.toId
import com.majeur.psclient.model.pokemon.TeamPokemon
import com.majeur.psclient.util.toId
import com.majeur.psclient.widget.CategoryAdapter
import timber.log.Timber
import kotlin.math.min


class EditTeamActivity : AppCompatActivity() {

    val glideHelper by lazy { GlideHelper(this) }
    val assetLoader by lazy { AssetLoader(this) }

    private lateinit var binding: ActivityEditTeamBinding

    private lateinit var team: Team
    private var teamNeedsName = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTeamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(Activity.RESULT_CANCELED, null)

        if (intent.hasExtra(INTENT_EXTRA_TEAM)) {
            team = intent.getSerializableExtra(INTENT_EXTRA_TEAM) as Team
        } else {
            team = Team("Unnamed team", emptyList(), BattleFormat.FORMAT_OTHER.toId())
            teamNeedsName = true
        }

        val spinner = Spinner(actionBar?.themedContext ?: this)
        spinner.adapter = object : CategoryAdapter(spinner.context) {
            override fun isCategoryItem(position: Int): Boolean {
                return getItem(position) is BattleFormat.Category
            }

            override fun getCategoryLabel(position: Int): String {
                return (getItem(position) as BattleFormat.Category).label!!
            }

            override fun getItemLabel(position: Int): String {
                return (getItem(position) as BattleFormat).label
            }
        }.also { it.addItem(BattleFormat.FORMAT_OTHER) }
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
                val format = adapterView.adapter.getItem(position) as BattleFormat
                team.format = format.toId()
            }
            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
        supportActionBar?.apply {
            customView = spinner
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowCustomEnabled(true)
            setDisplayShowTitleEnabled(false)
        }


        val battleFormats = intent.getSerializableExtra(INTENT_EXTRA_FORMATS) as List<BattleFormat.Category>?
        var indexInAdapter = 0
        battleFormats?.forEach { category ->
            (spinner.adapter as CategoryAdapter).addItem(category).also { indexInAdapter++ }
            (spinner.adapter as CategoryAdapter).addItems(category.formats.filter { it.isTeamNeeded }.also { it.forEach { e ->
                if (e.toId() == team.format?.toId()) spinner.setSelection(indexInAdapter, false)
                indexInAdapter++
            } })
        }

        binding.pager.adapter = object : FragmentPagerAdapter(supportFragmentManager,
                BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int) =
                    EditPokemonFragment.create(position, team.pokemons.getOrNull(position))

            override fun getCount() = min(team.pokemons.size + 1, MAX_TEAM_SIZE)

            override fun getPageTitle(position: Int) = "Slot ${position+1}"
        }
        binding.pagerTabs.tabIndicatorColor = ContextCompat.getColor(this, R.color.secondary)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_team, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_done -> {
            prepareTeam()
            val intent = Intent()
            intent.putExtra(INTENT_EXTRA_TEAM, team)
            setResult(Activity.RESULT_OK, intent)
            finish()
            true
        }
        R.id.home -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (teamNeedsName) {
            val dialogView: View = layoutInflater.inflate(R.layout.dialog_team_name, null)
            val editText = dialogView.findViewById<EditText>(R.id.edit_text_team_name)
            MaterialAlertDialogBuilder(this)
                    .setTitle("Team name")
                    .setPositiveButton("Done") { _: DialogInterface?, _: Int ->
                        val regex = "[{}:\",|\\[\\]]".toRegex()
                        var input = editText.text.toString().replace(regex, "")
                        if (input.isBlank()) input = "Unnamed team"
                        team.label = input
                        teamNeedsName = false
                    }
                    .setCancelable(false)
                    .setView(dialogView)
                    .show()
            editText.requestFocus()
        }
    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this)
                .setTitle("Changes will be lost")
                .setMessage("Are you sure you want to quit without applying changes ?")
                .setPositiveButton("Yes") { dialogInterface: DialogInterface?, i: Int -> finish() }
                .setNegativeButton("No", null)
                .show()
    }

    fun onPokemonUpdated(slotIndex: Int, pokemon: TeamPokemon?) {
        //pokemons[slotIndex] = pokemon TODO
        prepareTeam()
        Timber.d("onPokemonUpdated: $pokemon")
    }

    private fun prepareTeam() {
        TODO("Fix this")
        //team.pokemons = pokemons.filter { it != null }.toList()
        //pokemons.forEach { team.pokemons }
        //for (pokemon in pokemons) Utils.addNullSafe(team!!.pokemons, pokemon)
    }

    companion object {
        const val MAX_TEAM_SIZE = 6
        const val INTENT_REQUEST_CODE = 194
        const val INTENT_EXTRA_TEAM = "intent-extra-team"
        const val INTENT_EXTRA_FORMATS = "intent-extra-formats"
    }
}
