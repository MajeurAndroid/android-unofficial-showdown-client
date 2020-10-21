package com.majeur.psclient.ui.teambuilder

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.majeur.psclient.R
import com.majeur.psclient.databinding.ActivityTeamBuilderBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.io.GlideHelper
import com.majeur.psclient.model.common.BattleFormat
import com.majeur.psclient.model.common.Team
import com.majeur.psclient.model.common.toId

class TeamBuilderActivity : AppCompatActivity() {

    // As I am writing this code, fragment navigation framework does not manage fragments instance states.
    // The common fix is to use a shared LiveData or ViewModel, so as we are not using MVVM here, we use
    // activity as a common place to retrieve team pokemons.
    lateinit var team: Team

    val glideHelper by lazy { GlideHelper(this) }
    val assetLoader by lazy { AssetLoader(this) }

    private lateinit var binding: ActivityTeamBuilderBinding

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {

        override fun handleOnBackPressed() {
            MaterialAlertDialogBuilder(this@TeamBuilderActivity)
                    .setTitle("Changes will be lost")
                    .setMessage("Are you sure you want to quit without applying changes ?")
                    .setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                    .setNegativeButton("No", null)
                    .show()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeamBuilderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // TODO Retrieve cached formats if null
        @Suppress("UNCHECKED_CAST")
        val formats = intent.getSerializableExtra(INTENT_EXTRA_FORMATS) as List<BattleFormat.Category>?

        val navController = findNavController(R.id.nav_host_fragment)
        navController.setGraph(R.navigation.team_builder, bundleOf(
                TeamFragment.ARG_FORMATS to formats
        ))
        navController.addOnDestinationChangedListener { controller, destination, _ ->
            val isStartDestination = controller.graph.startDestination == destination.id
            onBackPressedCallback.isEnabled = isStartDestination
        }
        setupActionBarWithNavController(navController)


        team = intent.extras?.getSerializable(INTENT_EXTRA_TEAM) as Team?
                ?: Team("Unnamed team", emptyList(), BattleFormat.FORMAT_OTHER.toId())
        team.pokemons = team.pokemons.toMutableList() // Ensure we have a mutable list under the hood
        team.pokemons.forEach { poke ->
            poke.moves = poke.moves.toMutableList() // Ensure we have a mutable list under the hood
            for (i in poke.moves.size until 4) (poke.moves as MutableList<String>).add("") // Ensure we have a 4 element list
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    companion object {
        const val MAX_TEAM_SIZE = 6
        const val INTENT_REQUEST_CODE = 194
        const val INTENT_EXTRA_TEAM = "intent-extra-team"
        const val INTENT_EXTRA_FORMATS = "intent-extra-formats"
    }

}
