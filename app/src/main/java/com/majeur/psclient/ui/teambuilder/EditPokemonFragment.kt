package com.majeur.psclient.ui.teambuilder

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.majeur.psclient.R
import com.majeur.psclient.databinding.FragmentEditPokemonBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.io.GlideHelper
import com.majeur.psclient.model.Species
import com.majeur.psclient.model.common.Item
import com.majeur.psclient.model.common.Nature
import com.majeur.psclient.model.common.Stats
import com.majeur.psclient.model.common.Type
import com.majeur.psclient.model.common.Type.getResId
import com.majeur.psclient.model.pokemon.BasePokemon
import com.majeur.psclient.model.pokemon.DexPokemon
import com.majeur.psclient.model.pokemon.TeamPokemon
import com.majeur.psclient.ui.BaseFragment
import com.majeur.psclient.util.*
import com.majeur.psclient.widget.StatsTable
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class EditPokemonFragment : Fragment() {

    private val fragmentScope = BaseFragment.FragmentScope()
    private lateinit var assetLoader: AssetLoader
    private lateinit var glideHelper: GlideHelper

    private var _binding: FragmentEditPokemonBinding? = null
    private val binding get() = _binding!!

    private val moveInputs
        get() = binding.run { arrayOf(move1Input, move2Input, move3Input, move4Input) }

    private val clipboardManager
        get() = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private var mAttachedToActivity = false
    private var textHighlightColor = 0

    // Data
    private var mSlotIndex = 0
    private var mCurrentSpecies: Species? = null
    private var mCurrentItem: Item? = null
    private var mCurrentBaseStats: Stats? = null
    private val mCurrentEvs = Stats(0)
    private val mCurrentIvs = Stats(31)
    private var mCurrentAbility: String? = null
    private val mCurrentMoves = arrayOfNulls<String>(4)
    private var mCurrentNature: Nature = Nature.DEFAULT
    private var mHasPokemonData = false
    private val mACETFocusListener = OnFocusChangeListener { view: View, hasFocus: Boolean ->
        val textView = view as AutoCompleteTextView
        if (hasFocus && textView.length() == 0) textView.showDropDown()
        if (!hasFocus && activity != null) Utils.hideSoftInputMethod(activity)
    }

    private fun makeSnackbar(message: String, indefinite: Boolean = false) {
        Snackbar.make(binding.root, message, if (indefinite) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_LONG).show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mAttachedToActivity = true
        val activity = context as EditTeamActivity
        assetLoader = activity.assetLoader
        glideHelper = activity.glideHelper
        textHighlightColor = Utils.alphaColor(ContextCompat.getColor(context, R.color.secondary), 0.45f)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mSlotIndex = requireArguments().getInt(ARG_SLOT_INDEX)
        lifecycle.addObserver(fragmentScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(fragmentScope)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentEditPokemonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO mettre a la fin
        fragmentScope.launch {
            assetLoader.allSpecies("")?.let { allSpecies ->
                binding.speciesInput.setAdapter(SpeciesAdapter(allSpecies, textHighlightColor))
            }
        }
        fragmentScope.launch {
            assetLoader.allItems("")?.let { allItems -> // TODO Item generic type was Any* (Object in java)
                binding.itemInput.setAdapter(FilterableAdapter<Item>(allItems, textHighlightColor))
            }
        }
        binding.speciesInput.apply {
            threshold = 1
            dropDownWidth = Utils.dpToPx(196f)
            onItemClickListener = AdapterView.OnItemClickListener { adapterView: AdapterView<*>, _: View?, i: Int, _: Long ->
                val adapter = adapterView.adapter
                val newSpecies = adapter.getItem(i) as Species
                trySpecies(newSpecies.id)
            }
            addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(editable: Editable) {
                    val regex = "[,|\\[\\]]".toRegex() // escape |,[] characters TODO: only accept A-Z0-9-
                    val input = editable.replace(regex, "")
                    if (editable.length != input.length) {
                        editable.replace(0, editable.length, input)
                    } else {
                        notifyPokemonDataChanged()
                    }
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
                    binding.statsTable.setLevel(currentLevel)
                    notifyPokemonDataChanged()
                }
            })
        }
        binding.shiny.apply {
            setOnCheckedChangeListener { _: CompoundButton?, _: Boolean ->
                updatePokemonSprite()
                notifyPokemonDataChanged()
            }
        }
        binding.happinessInput.apply {
            onFocusChangeListener = OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (!hasFocus && binding.happinessInput.length() == 0) binding.happinessInput.setText("255")
            }
            addTextChangedListener(RangeNumberTextWatcher(0, 255))
            addTextChangedListener(object : SimpleTextWatcher() {
                override fun afterTextChanged(editable: Editable) {
                    notifyPokemonDataChanged()
                }
            })
        }
        binding.abilitySelector.apply {
            onItemSelectedListener = object : SimpleOnItemSelectedListener() {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                    val adapter = adapterView.adapter
                    val readableAbility = adapter.getItem(i) as String
                    mCurrentAbility = readableAbility.removeSuffix("(Hidden)").toId()
                    notifyPokemonDataChanged()
                }
            }
        }
        binding.itemInput.apply {
            onFocusChangeListener = mACETFocusListener
            onItemClickListener = AdapterView.OnItemClickListener { adapterView: AdapterView<*>, view14: View?, i: Int, l: Long ->
                val adapter = adapterView.adapter
                mCurrentItem = adapter.getItem(i) as Item
                notifyPokemonDataChanged()
            }
        }
        moveInputs.forEachIndexed { index, textView ->
            textView.threshold = 1
            textView.onFocusChangeListener = mACETFocusListener
            textView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, clickedView, i, _ ->
                mCurrentMoves[index] = adapterView.adapter.getItem(i) as String
                notifyPokemonDataChanged()
                // Check if we have the full name to display to user
                if (clickedView.tag is MovesAdapter.ViewHolder) {
                    val text = (clickedView.tag as MovesAdapter.ViewHolder).nameView.text
                    if (text.isNotEmpty() && Character.isUpperCase(text[0]))
                        textView.setText(text.toString()) // Prevents highlight spans
                }
                if (index < 3) {
                    if (moveInputs[index + 1].length() == 0) moveInputs[index + 1].requestFocus() else textView.clearFocus()
                } else {
                    textView.clearFocus()
                }
            }
        }
        binding.statsTable.apply {
            setRowClickListener { _: StatsTable?, rowName: String?, index: Int ->
                val dialogAlreadyOpenned = requireFragmentManager().findFragmentByTag(EditStatDialog.FRAGMENT_TAG) != null
                if (dialogAlreadyOpenned || mCurrentBaseStats == null || mCurrentNature == null) return@setRowClickListener
                val dialog = EditStatDialog.newInstance(rowName, mCurrentBaseStats!!.get(index),
                        mCurrentEvs.get(index), mCurrentIvs.get(index), currentLevel,
                        mCurrentNature!!.getStatModifier(index), mCurrentEvs.sum())
                dialog.setTargetFragment(this@EditPokemonFragment, 0)
                dialog.show(requireFragmentManager(), EditStatDialog.FRAGMENT_TAG)
            }
        }
        binding.natureSelector.apply {
            adapter = ArrayAdapter(view.context, android.R.layout.simple_dropdown_item_1line, Nature.ALL)
            onItemSelectedListener = object : SimpleOnItemSelectedListener() {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                    val adapter = adapterView.adapter as ArrayAdapter<Nature>
                    mCurrentNature = adapter.getItem(i) ?: Nature.DEFAULT
                    binding.statsTable.setNature(mCurrentNature)
                    notifyPokemonDataChanged()
                }
            }
        }
        binding.hpTypeSelector.apply {
            adapter = ArrayAdapter(view.context, android.R.layout.simple_dropdown_item_1line, Type.HP_TYPES)
            onItemSelectedListener = object : SimpleOnItemSelectedListener() {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                    val adapter = adapterView.adapter as ArrayAdapter<String>
                    mCurrentIvs.setForHpType(adapter.getItem(i))
                    binding.statsTable.setIVs(mCurrentIvs)
                    notifyPokemonDataChanged()
                }
            }
        }
        binding.clearButton.apply {
            setOnClickListener { clearSelectedSpecies() }
        }
        binding.exportButton.apply {
            setOnClickListener {
                if (!mHasPokemonData) return@setOnClickListener
                val pokemon = buildPokemon()
                val text = ShowdownTeamParser.fromPokemon(pokemon)
                val clip = ClipData.newPlainText("Exported Pokemon", text)
                clipboardManager.setPrimaryClip(clip)
                Snackbar.make(binding.root, "Pokemon successfully exported to clipboard", Snackbar.LENGTH_LONG).show()
            }
        }
        binding.importButton.apply {
            setOnClickListener {
                val clip = clipboardManager.primaryClip
                if (clip == null) {
                    makeSnackbar("There is nothing in clipboard")
                    return@setOnClickListener
                } else if (!clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || clip.itemCount == 0) {
                    makeSnackbar("There is nothing that looks like a Pokemon in clipboard")
                    return@setOnClickListener
                }
                val pokemon = ShowdownTeamParser.parsePokemon(clip.getItemAt(0).text.toString()) { name: String ->
                    assetLoader.dexPokemonNonSuspend(name.toId())
                }
                if (pokemon == null) {
                    makeSnackbar("Could not parse pokemon from clipboard data")
                    return@setOnClickListener
                }
                fragmentScope.launch {
                    val dexPokemon = assetLoader.dexPokemon(pokemon.species.toId())
                    if (dexPokemon == null) { // This pokemon does not have an entry in our dex.json
                        makeSnackbar("The pokemon you imported does not exist in current pokedex")
                        return@launch
                    }
                    if (mHasPokemonData) clearSelectedSpecies()
                    bindExistingPokemon(pokemon) // Binding our data
                    bindDexPokemon(dexPokemon) // Setting data from dex
                    mHasPokemonData = true
                    toggleInputViewsEnabled(true)
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toggleInputViewsEnabled(false)
        val pokemon = requireArguments().getSerializable(ARG_PKMN) as TeamPokemon?
        if (pokemon != null) {
            fragmentScope.launch {
                val dexPokemon = assetLoader.dexPokemon(pokemon.species.toId())
                        ?: return@launch // This pokemon does not have an entry in our dex.json
                bindExistingPokemon(pokemon) // Binding our data
                bindDexPokemon(dexPokemon) // Setting data from dex
                mHasPokemonData = true
                toggleInputViewsEnabled(true)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        mAttachedToActivity = false
    }

    private fun trySpecies(species: String) {
        fragmentScope.launch {
            val dexPokemon = assetLoader.dexPokemon(species.toId())
            if (dexPokemon == null) {
                binding.speciesInput.setText(mCurrentSpecies?.name)
            } else {
                bindDexPokemon(dexPokemon)
                mHasPokemonData = true
                toggleInputViewsEnabled(true)
            }
        }
    }

    private fun bindDexPokemon(dexPokemon: DexPokemon) {
        mCurrentSpecies = Species()
        mCurrentSpecies!!.id = dexPokemon.species.toId()
        mCurrentSpecies!!.name = dexPokemon.species
        updatePokemonSprite()
        binding.speciesInput.setText(dexPokemon.species)

        binding.type1.apply {
            setImageResource(getResId(dexPokemon.firstType))
        }
        binding.type2.apply {
            if (dexPokemon.secondType != null) setImageResource(getResId(dexPokemon.secondType))
            else setImageDrawable(null)
        }

        val abilities = dexPokemon.abilities.toMutableList()
        dexPokemon.hiddenAbility?.let { abilities.add("$it (Hidden)") }
        binding.abilitySelector.apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, abilities)
        }
        if (mCurrentAbility == null) {
            mCurrentAbility = abilities[0]
        } else {
            abilities.forEachIndexed { index, ability ->
                if (mCurrentAbility!!.toId().contains(ability.toId()))
                    binding.abilitySelector.setSelection(index)
            }
        }

        fragmentScope.launch {
            val moves = assetLoader.learnset(mCurrentSpecies!!.id) ?: return@launch
            moveInputs.forEach {
                it.setAdapter(MovesAdapter(moves, textHighlightColor))
            }
        }

        val stats = dexPokemon.baseStats
        mCurrentBaseStats = stats
        binding.statsTable.setBaseStats(stats)
    }

    private fun bindExistingPokemon(pokemon: TeamPokemon) {
        if (!pokemon.species.equals(pokemon.name, ignoreCase = true)) binding.nameInput.setText(pokemon.name)
        binding.levelInput.setText(pokemon.level.toString())
        binding.shiny.isChecked = pokemon.shiny
        binding.happinessInput.setText(pokemon.happiness.toString())
        mCurrentAbility = pokemon.ability
        val itemAdapter = binding.itemInput.adapter as FilterableAdapter<Item>?
        if (itemAdapter != null) {
            (0..itemAdapter.count).map { itemAdapter.getItem(it) }.forEach { item ->
                if (item.id == pokemon.item?.toId()) {
                    mCurrentItem = item
                    binding.itemInput.setText(item.name)
                }
            }
        } else {
            pokemon.item?.let {
                mCurrentItem = Item().apply { name = it }.apply { id = it }
            }
        }
        for (i in mCurrentMoves.indices) {
            mCurrentMoves[i] = pokemon.moves.getOrNull(i)
            if (mCurrentMoves[i] == null) continue
            fragmentScope.launch {
                val details = assetLoader.moveDetails(mCurrentMoves[i]!!) ?: return@launch
                moveInputs[i].setText(details.name)
            }
        }
        mCurrentEvs.set(pokemon.evs)
        mCurrentIvs.set(pokemon.ivs)
        binding.statsTable.setEVs(mCurrentEvs)
        binding.statsTable.setIVs(mCurrentIvs)
        pokemon.nature?.run {
            mCurrentNature = Nature.ALL.firstOrNull { it.name.toId() == toId() } ?: Nature.DEFAULT
            binding.natureSelector.setSelection(Nature.ALL.indexOf(mCurrentNature))
        }
    }

    private fun clearSelectedSpecies() {
        toggleInputViewsEnabled(false)
        mHasPokemonData = false
        mCurrentSpecies = null
        updatePokemonSprite()
        binding.apply {
            speciesInput.text.clear()
            type1.setImageDrawable(null)
            type2.setImageDrawable(null)
            nameInput.text.clear()
            levelInput.setText("100")
            shiny.isChecked = false
            happinessInput.setText("255")
            abilitySelector.adapter = null
            itemInput.text.clear()
            statsTable.clear()
            natureSelector.setSelection(0)
            speciesInput.requestFocus()
            scrollView.smoothScrollTo(0, 0)
        }
        mCurrentAbility = null
        mCurrentItem = null
        mCurrentMoves.indices.forEach { i -> mCurrentMoves[i] = null }
        moveInputs.forEach { it.setAdapter(null) }
        mCurrentEvs.setAll(0)
        mCurrentIvs.setAll(31)
        mCurrentBaseStats = null
        mCurrentNature = Nature.DEFAULT
        updatePokemonNoData()
    }

    private fun updatePokemonSprite() {
        if (mCurrentSpecies == null) binding.sprite.setImageResource(R.drawable.placeholder_pokeball)
        else glideHelper.loadDexSprite(BasePokemon().also { it.species = mCurrentSpecies!!.name },
                binding.shiny.isChecked, binding.sprite)
    }

    private fun toggleInputViewsEnabled(enabled: Boolean) {
        binding.apply {
            clearButton.isEnabled = enabled
            clearButton.icon.alpha = if (enabled) 255 else 125
            nameInput.isEnabled = enabled
            levelInput.isEnabled = enabled
            happinessInput.isEnabled = enabled
            shiny.isEnabled = enabled
            abilitySelector.isEnabled = enabled
            itemInput.isEnabled = enabled
            statsTable.isEnabled = enabled
            natureSelector.isEnabled = enabled
            hpTypeSelector.isEnabled = enabled
            exportButton.isEnabled = enabled
        }
        moveInputs.forEach { it.isEnabled = enabled }
    }

    fun onStatModified(stat: String, ev: Int, iv: Int) {
        mCurrentEvs.set(stat, ev)
        mCurrentIvs.set(stat, iv)
        binding.statsTable.setEVs(mCurrentEvs)
        binding.statsTable.setIVs(mCurrentIvs)
        notifyPokemonDataChanged()
    }

    private fun notifyPokemonDataChanged() {
        if (!mHasPokemonData || !mAttachedToActivity) return
        val activity = requireActivity() as EditTeamActivity
        activity.onPokemonUpdated(mSlotIndex, buildPokemon())
    }

    private fun buildPokemon(): TeamPokemon {
        val pokemon = TeamPokemon(mCurrentSpecies!!.name)
        pokemon.name = if (binding.nameInput.length() > 0) binding.nameInput.text
                .toString() else null
        pokemon.level = currentLevel
        pokemon.happiness = currentHappiness
        pokemon.shiny = binding.shiny.isChecked
        pokemon.ability = mCurrentAbility?.toId()
        pokemon.item = if (mCurrentItem != null) mCurrentItem!!.id else ""
        pokemon.moves = mCurrentMoves.toList().filterNotNull()
        pokemon.ivs = mCurrentIvs
        pokemon.evs = mCurrentEvs
        pokemon.nature = mCurrentNature.name
        return pokemon
    }

    private fun updatePokemonNoData() {
        if (!mAttachedToActivity) return
        val activity = requireActivity() as EditTeamActivity
        activity.onPokemonUpdated(mSlotIndex, null)
    }

    private val currentLevel get() = binding.levelInput.text.toString().toIntOrNull() ?: 100

    private val currentHappiness get() = binding.happinessInput.text.toString().toIntOrNull() ?: 255


    inner class SpeciesAdapter(
            objects: List<Species?>,
            highlightColor: Int
    ) : FilterableAdapter<Species?>(objects, highlightColor) {

        private val icWidth = Utils.dpToPx(32f)
        private val icHeight = Utils.dpToPx(32f * 3f / 4f)

        override fun getView(position: Int, view: View?, parent: ViewGroup): View {
            val convertView = view ?: layoutInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false)

            val species = getItem(position)!!
            val textView = convertView as TextView
            textView.text = species.name
            highlightMatch(textView)
            textView.setCompoundDrawables(null, null, null, null) // Remove eventual previous icon

            (convertView.tag as Job?)?.cancel()
            convertView.tag = fragmentScope.launch {
                val dexIcon = assetLoader.dexIcon(species.id)
                val drawable = BitmapDrawable(textView.resources, dexIcon).also { it.setBounds(0, 0, icWidth, icHeight) }
                textView.setCompoundDrawables(drawable, null, null, null)
            }
            return convertView
        }

        override fun matchConstraint(constraint: String?, candidate: Species?): Boolean {
            return candidate!!.name.toLowerCase().contains(constraint!!.toLowerCase())
        }
    }

    inner class MovesAdapter(
            moveIds: Collection<String?>?,
            highlightColor: Int
    ) : FilterableAdapter<String?>(moveIds, highlightColor) {


        inner class ViewHolder(view: View) {
            var job: Job? = null
            val nameView: TextView = view.findViewById(R.id.name_view)
            val detailsView: TextView = view.findViewById(R.id.details_view)
            val typeView: ImageView = view.findViewById(R.id.type_view)
            val categoryView: ImageView = view.findViewById(R.id.category_view)
        }

        override fun getView(position: Int, view: View?, parent: ViewGroup): View {
            val convertView = view ?: layoutInflater.inflate(R.layout.list_item_move, parent, false).also { it.tag = ViewHolder(it) }
            val moveId = getItem(position)!!

            (convertView.tag as ViewHolder).apply {
                nameView.setText(moveId, TextView.BufferType.SPANNABLE)
                highlightMatch(nameView)
                detailsView.text = buildDetailsText(-1, -1, -1)
                typeView.setImageDrawable(null)
                typeView.animate().cancel()
                typeView.alpha = 0f
                categoryView.setImageDrawable(null)
                categoryView.animate().cancel()
                categoryView.alpha = 0f
                job?.cancel()
                job = fragmentScope.launch {
                    val details = assetLoader.moveDetails(moveId) ?: return@launch
                    nameView.setText(details.name, TextView.BufferType.SPANNABLE)
                    highlightMatch(nameView)
                    detailsView.text = buildDetailsText(details.pp, details.basePower, details.accuracy)
                    typeView.setImageResource(getResId(details.type))
                    typeView.animate().alpha(1f).start()
                    categoryView.setImageDrawable(CategoryDrawable(details.category))
                    categoryView.animate().alpha(1f).start()
                }
            }
            return convertView
        }

        override fun highlightMatch(textView: TextView) {
            val constraint = currentConstraint ?: return
            var text = textView.text.toString().toLowerCase()
            val spaceIndex = text.indexOf(' ')
            text = text.replace(" ", "")
            if (!text.contains(constraint)) return
            var startIndex = text.indexOf(constraint)
            if (spaceIndex in 1..startIndex) startIndex++
            var endIndex = startIndex + constraint.length
            if (spaceIndex > 0 && startIndex < spaceIndex && endIndex > spaceIndex) endIndex++
            val spannable = textView.text as Spannable
            spannable.setSpan(BackgroundColorSpan(highlightColor), startIndex, endIndex,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        private fun buildDetailsText(pp: Int, bp: Int, acc: Int): String {
            return ("PP: " + (if (pp >= 0) pp else "–") + ", BP: " + (if (bp > 0) bp else "–")
                    + ", AC: " + if (acc > 0) acc else "–")
        }

        override fun prepareConstraint(constraint: CharSequence): String {
            return constraint.toString().toLowerCase().replace(" ", "")
        }

        override fun matchConstraint(constraint: String?, candidate: String?): Boolean {
            return candidate!!.contains(constraint!!)
        }
    }

    companion object {

        private const val ARG_SLOT_INDEX = "arg-slot-index"

        private const val ARG_PKMN = "arg-pkmn"

        fun create(slotIndex: Int, pkmn: TeamPokemon?): EditPokemonFragment {
            val fragment = EditPokemonFragment()
            val args = Bundle()
            args.putInt(ARG_SLOT_INDEX, slotIndex)
            args.putSerializable(ARG_PKMN, pkmn)
            fragment.arguments = args
            return fragment
        }
    }
}