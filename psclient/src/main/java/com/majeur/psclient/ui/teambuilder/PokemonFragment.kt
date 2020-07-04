package com.majeur.psclient.ui.teambuilder

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Editable
import android.view.*
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.majeur.psclient.R
import com.majeur.psclient.databinding.FragmentTbPokemonBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.io.GlideHelper
import com.majeur.psclient.model.common.Nature
import com.majeur.psclient.model.common.Stats
import com.majeur.psclient.model.common.Type
import com.majeur.psclient.model.common.Type.getResId
import com.majeur.psclient.ui.BaseFragment
import com.majeur.psclient.util.*
import com.majeur.psclient.widget.StatsTable
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class PokemonFragment : Fragment() {

    private val fragmentScope = BaseFragment.FragmentScope()
    private lateinit var assetLoader: AssetLoader
    private lateinit var glideHelper: GlideHelper

    private var _binding: FragmentTbPokemonBinding? = null
    private val binding get() = _binding!!

    private val moveInputs
        get() = binding.run { arrayOf(move1Input, move2Input, move3Input, move4Input) }

    private val clipboardManager
        get() = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // See TeamBuilderActivity field declaration comment
    private val pokemon
        get() = (requireActivity() as TeamBuilderActivity).team.pokemons[slotIndex]

    private var slotIndex = 0
    private var baseStats: Stats? = null

    private fun makeSnackbar(message: String, indefinite: Boolean = false) {
        Snackbar.make(binding.root, message, if (indefinite) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG).show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = context as TeamBuilderActivity
        assetLoader = activity.assetLoader
        glideHelper = activity.glideHelper
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        slotIndex = requireArguments().getInt(ARG_SLOT_INDEX)
        lifecycle.addObserver(fragmentScope)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            containerColor = ContextCompat.getColor(requireActivity(), R.color.background)
            duration = 500
            setPathMotion(MaterialArcMotion())
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            interpolator = FastOutSlowInInterpolator()
        }

        setFragmentResultListener(ItemsFragment.RESULT_KEY) { _, bundle ->
            val item = bundle.getString(ItemsFragment.RESULT_ITEM)
            if (item != null) {
                pokemon.item = item
                binding.itemInput.text = item
            }
        }

        setFragmentResultListener(MovesFragment.RESULT_KEY) { _, bundle ->
            val moveName = bundle.getString(MovesFragment.RESULT_MOVE)
            val moveSlot = bundle.getInt(MovesFragment.RESULT_SLOT)
            if (moveName != null) {
                // We know it's a 4 elements mutable list (See TeamBuilderActivity)
                (pokemon.moves as MutableList<String>)[moveSlot] = moveName
                moveInputs[moveSlot].text = moveName
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(fragmentScope)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options_menu_tb_pokemon, menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
        R.id.action_paste -> run {
            val clip = clipboardManager.primaryClip
            if (clip == null || !clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || clip.itemCount == 0) {
                makeSnackbar("There is nothing that looks like a Pokemon in clipboard")
                return@run true
            }
            fragmentScope.launch {
                val rawPokemon = clip.getItemAt(0).text.toString()
                val poke = SmogonTeamParser.parsePokemon(rawPokemon, assetLoader)
                if (poke == null) {
                    makeSnackbar("Could not parse pokemon from clipboard data")
                    return@launch
                }
                pokemon.apply {
                    species = poke.species
                    name = poke.name
                    item = poke.item
                    ability = poke.ability
                    moves = poke.moves
                    nature = poke.nature
                    evs = poke.evs
                    gender = poke.gender
                    ivs = poke.ivs
                    shiny = poke.shiny
                    level = poke.level
                    happiness = poke.happiness
                    hpType = poke.hpType
                    pokeball = poke.pokeball
                }
                toggleInputViewsEnabled(false)
                bindToPokemon()
                trySpecies(poke.species, poke.ability, poke.moves)
            }
            true
        }
        R.id.action_copy -> {
            if (pokemon.species.isBlank()) {
                makeSnackbar("Choose a species first")
            } else {
                fragmentScope.launch {
                    val text = SmogonTeamBuilder.buildPokemon(assetLoader, pokemon)
                    val clip = ClipData.newPlainText(pokemon.species, text)
                    clipboardManager.setPrimaryClip(clip)
                    makeSnackbar("${pokemon.species} copied to clipboard")
                }
            }
            true
        }
        else -> false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTbPokemonBinding.inflate(inflater, container, false)
        ViewCompat.setTransitionName(binding.root, "content_$slotIndex")
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.speciesInput.apply {
            threshold = 1
            dropDownWidth = Utils.dpToPx(196f)
            onItemClickListener = AdapterView.OnItemClickListener { adapterView: AdapterView<*>, _: View?, i: Int, _: Long ->
                val newSpecies = adapterView.adapter.getItem(i) as String
                trySpecies(newSpecies.toId())
            }
            setAdapter(SpeciesAdapter())
        }
        binding.shiny.apply {
            setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean ->
                pokemon.shiny = checked
                updatePokemonSprite()
            }
        }
        binding.nameInput.apply {
            addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(editable: Editable?) {
                    pokemon.name = if (editable?.isNotBlank() == true) editable.toString() else ""
                }
            })
        }
        binding.levelInput.apply {
            onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (!hasFocus && binding.levelInput.length() == 0) binding.levelInput.setText("100")
            }
            addTextChangedListener(RangeNumberTextWatcher(1, 100))
            addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(editable: Editable) {
                    val level = editable.toString().toIntOrNull() ?: 100
                    binding.statsTable.setLevel(level)
                    pokemon.level = level
                }
            })
        }
        binding.happinessInput.apply {
            onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (!hasFocus && binding.happinessInput.length() == 0) binding.happinessInput.setText("255")
            }
            addTextChangedListener(RangeNumberTextWatcher(0, 255))
            addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(editable: Editable) {
                    val happiness = editable.toString().toIntOrNull() ?: 255
                    pokemon.happiness = happiness
                }
            })
        }
        binding.abilitySelector.apply {
            onItemSelectedListener = object : SimpleOnItemSelectedListener() {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                    val ability = adapter.getItem(i) as String
                    pokemon.ability = ability.removeSuffix(" (Hidden)")
                }
            }
        }
        binding.itemInput.apply {
            setOnClickListener {
                findNavController().navigate(R.id.action_pokemon_frag_to_item_choice_frag)
            }
        }
        moveInputs.forEachIndexed { moveSlot, textView ->
            textView.setOnClickListener {
                val bundle = bundleOf(
                        MovesFragment.ARG_SPECIES to pokemon.species.toId(),
                        MovesFragment.ARG_SLOT to moveSlot
                )
                findNavController().navigate(R.id.action_pokemon_frag_to_move_choice_frag, bundle)
            }
        }
        binding.statsTable.apply {
            setRowClickListener { _: StatsTable?, rowName: String?, index: Int ->
                val dialogAlreadyOpened = childFragmentManager.findFragmentByTag(EditStatDialog.FRAGMENT_TAG) != null
                if (dialogAlreadyOpened || baseStats == null) return@setRowClickListener
                val level = binding.levelInput.text.toString().toIntOrNull() ?: 100
                val nature = binding.natureSelector.selectedItem as? Nature? ?: Nature.DEFAULT
                val dialog = EditStatDialog.newInstance(rowName, baseStats!!.get(index),
                        pokemon.evs.get(index), pokemon.ivs.get(index), level,
                        nature.getStatModifier(index), pokemon.evs.sum())
                dialog.show(childFragmentManager, EditStatDialog.FRAGMENT_TAG)
            }
        }
        binding.natureSelector.apply {
            adapter = ArrayAdapter(view.context, android.R.layout.simple_dropdown_item_1line, Nature.ALL)
            onItemSelectedListener = object : SimpleOnItemSelectedListener() {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                    @Suppress("UNCHECKED_CAST")
                    val adapter = adapterView.adapter as ArrayAdapter<Nature>
                    val nature = adapter.getItem(i) ?: Nature.DEFAULT
                    binding.statsTable.setNature(nature)
                    pokemon.nature = nature.name.toId()
                }
            }
        }
        binding.hpTypeSelector.apply {
            adapter = ArrayAdapter(view.context, android.R.layout.simple_dropdown_item_1line, Type.HP_TYPES)
            onItemSelectedListener = object : SimpleOnItemSelectedListener() {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                    @Suppress("UNCHECKED_CAST")
                    val adapter = adapterView.adapter as ArrayAdapter<String>
                    val hpType = adapter.getItem(i) ?: ""
                    pokemon.ivs.setForHpType(hpType)
                    binding.statsTable.setIVs(pokemon.ivs)
                    pokemon.hpType = hpType
                }
            }
        }
        toggleInputViewsEnabled(false)
        if (pokemon.species.isNotBlank()) {
            bindToPokemon()
            trySpecies(pokemon.species, pokemon.ability, pokemon.moves)
        } else {
            binding.speciesInput.requestFocus()
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.speciesInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun bindToPokemon() {
        updatePokemonSprite()
        binding.speciesInput.apply {
            setText(pokemon.species)
        }
        binding.shiny.apply {
            isChecked = pokemon.shiny
        }
        binding.nameInput.apply {
            setText(pokemon.name)
        }
        binding.levelInput.apply {
            setText(pokemon.level.toString())
        }
        binding.happinessInput.apply {
            setText(pokemon.happiness.toString())
        }
        binding.itemInput.apply {
            text = pokemon.item.or("None")
        }
        moveInputs.forEachIndexed { moveSlot, textView ->
            val moveName = pokemon.moves[moveSlot]
            textView.text =  moveName.or("None")
        }
        binding.statsTable.apply {
            setEVs(pokemon.evs)
            setIVs(pokemon.ivs)
        }
        binding.natureSelector.apply {
            val selection = Nature.ALL.indexOfFirst { it.name.toId() == pokemon.nature.toId() }
            setSelection(if (selection > 0) selection else 0)
        }
        binding.hpTypeSelector.apply {
            val selection = Type.HP_TYPES.indexOfFirst { it.toId() == pokemon.hpType.toId() }
            if (selection > 0) setSelection(selection)
        }
    }

    private fun trySpecies(species: String, ability: String? = null, moves: List<String>? = null) {
        fragmentScope.launch {
            val dexPokemon = assetLoader.dexPokemon(species.toId())
            if (dexPokemon == null) {
                makeSnackbar("Pokemon '$species' does not exist in current dex")
                binding.speciesInput.setText("")
            } else {
                val moveDetails = if (moves?.isNotEmpty() == true)
                    assetLoader.movesDetails(*moves.map { it.toId() }.toTypedArray()) else null

                pokemon.species = dexPokemon.species
                updatePokemonSprite()

                binding.type1.apply {
                    setImageResource(getResId(dexPokemon.firstType))
                }
                binding.type2.apply {
                    if (dexPokemon.secondType != null) setImageResource(getResId(dexPokemon.secondType))
                    else setImageDrawable(null)
                }
                if (pokemon.item.isNotBlank())
                    binding.itemInput.text = assetLoader.item(pokemon.item.toId())?.name ?: "None"

                val abilities = dexPokemon.abilities.toMutableList()
                dexPokemon.hiddenAbility?.let { abilities.add("$it (Hidden)") }
                binding.abilitySelector.apply {
                    adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, abilities)
                    val pos = if (ability == null) 0 else abilities.indexOfFirst { it.toId().contains(ability.toId()) }
                    setSelection(if (pos < 0) 0 else pos)
                }

                moveDetails?.forEachIndexed { slot, details ->
                    if (details == null) return@forEachIndexed
                    moveInputs[slot].text = details.name
                }

                val stats = dexPokemon.baseStats
                baseStats = stats
                binding.statsTable.setBaseStats(stats)
                toggleInputViewsEnabled(true)
            }
        }
    }

    private fun updatePokemonSprite() {
        if (pokemon.species.isBlank()) binding.sprite.setImageResource(R.drawable.placeholder_pokeball)
        else glideHelper.loadDexSprite(pokemon,
               pokemon.shiny, binding.sprite)
    }

    private fun toggleInputViewsEnabled(enabled: Boolean) {
        binding.apply {
            nameInput.isEnabled = enabled
            levelInput.isEnabled = enabled
            happinessInput.isEnabled = enabled
            shiny.isEnabled = enabled
            abilitySelector.isEnabled = enabled
            itemInput.isEnabled = enabled
            statsTable.isEnabled = enabled
            natureSelector.isEnabled = enabled
            hpTypeSelector.isEnabled = enabled
        }
        moveInputs.forEach { it.isEnabled = enabled }
    }

    fun onStatModified(stat: String, ev: Int, iv: Int) {
        pokemon.evs.set(stat, ev)
        pokemon.ivs.set(stat, iv)
        binding.statsTable.setEVs(pokemon.evs)
        binding.statsTable.setIVs(pokemon.ivs)
    }

    inner class SpeciesAdapter : BaseAdapter(), Filterable {

        private val icWidth = Utils.dpToPx(32f)
        private val icHeight = Utils.dpToPx(32f * 3f / 4f)

        private var adapterList = emptyList<String>()

        private val myFilter by lazy {
            object : Filter() {

                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val species = assetLoader.allSpeciesNonSuspend(constraint?.toString() ?: "")
                    return FilterResults().apply {
                        count = species?.size ?: 0
                        values = species
                    }
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    @Suppress("UNCHECKED_CAST")
                    adapterList = if (results != null && results.count > 0) results.values as List<String> else emptyList()
                    notifyDataSetChanged()
                }
            }
        }

        override fun getFilter() = myFilter

        override fun getView(position: Int, view: View?, parent: ViewGroup): View {
            val convertView = view ?: layoutInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false)

            val species = getItem(position)
            val textView = convertView as TextView
            textView.text = species
           // highlightMatch(textView)
            textView.setCompoundDrawables(null, null, null, null) // Remove eventual previous icon

            (convertView.tag as Job?)?.cancel()
            convertView.tag = fragmentScope.launch {
                val dexIcon = assetLoader.dexIcon(species.toId())
                val drawable = BitmapDrawable(textView.resources, dexIcon).also { it.setBounds(0, 0, icWidth, icHeight) }
                textView.setCompoundDrawables(drawable, null, null, null)
            }
            return convertView
        }

        override fun getCount() = adapterList.size

        override fun getItem(position: Int) = adapterList[position]

        override fun getItemId(position: Int) = 0L
    }

    companion object {
        const val ARG_SLOT_INDEX = "arg-slot-index"
    }
}