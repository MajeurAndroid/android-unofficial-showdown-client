package com.majeur.psclient.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.majeur.psclient.R
import com.majeur.psclient.databinding.FragmentBattleBinding
import com.majeur.psclient.io.AssetLoader
import com.majeur.psclient.io.BattleAudioManager
import com.majeur.psclient.io.GlideHelper
import com.majeur.psclient.model.battle.*
import com.majeur.psclient.model.common.Colors
import com.majeur.psclient.model.common.Colors.healthColor
import com.majeur.psclient.model.common.Colors.statusColor
import com.majeur.psclient.model.common.Stats
import com.majeur.psclient.model.common.Type
import com.majeur.psclient.model.pokemon.BasePokemon
import com.majeur.psclient.model.pokemon.BattlingPokemon
import com.majeur.psclient.model.pokemon.SidePokemon
import com.majeur.psclient.service.ShowdownService
import com.majeur.psclient.service.observer.BattleRoomMessageObserver
import com.majeur.psclient.util.*
import com.majeur.psclient.util.html.Html
import com.majeur.psclient.widget.BattleDecisionWidget
import com.majeur.psclient.widget.BattleLayout
import com.majeur.psclient.widget.BattleTipPopup
import kotlinx.coroutines.launch

class BattleFragment : BaseFragment(), BattleRoomMessageObserver.UiCallbacks, View.OnClickListener {

    private val observer get() = service!!.battleMessageObserver

    private lateinit var glideHelper: GlideHelper
    private lateinit var audioManager: BattleAudioManager
    private lateinit var assetLoader: AssetLoader

    private lateinit var inactiveBattleOverlayDrawable: InactiveBattleOverlayDrawable

    private lateinit var battleTipPopup: BattleTipPopup
    private var lastDecisionRequest: BattleDecisionRequest? = null
    private var timerEnabled = false
    private var soundEnabled = false
    private var wasPlayingBattleMusicWhenPaused = false

    private var _binding: FragmentBattleBinding? = null
    private val binding get() = _binding!!

    private var _observedRoomId: String? = null
    var observedRoomId: String?
        get() = _observedRoomId
        set(observedRoomId) {
            _observedRoomId = observedRoomId
            observer.observedRoomId = observedRoomId
        }

    val battleRunning get() = observer.battleRunning
    val isReplay get() = observer.isReplay

    override fun onAttach(context: Context) {
        super.onAttach(context)
        glideHelper = mainActivity.glideHelper
        assetLoader = mainActivity.assetLoader
        battleTipPopup = BattleTipPopup(context)
        battleTipPopup.bindPopupListener = onBindPopupListener
        audioManager = BattleAudioManager(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentBattleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        service?.battleMessageObserver?.uiCallbacks = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inactiveBattleOverlayDrawable = InactiveBattleOverlayDrawable(resources)
        binding.apply {
            battleLog.movementMethod = LinkMovementMethod()
            battleLog.setText("", TextView.BufferType.EDITABLE) // Setting the editable buffer type
            overlayImage.setImageDrawable(inactiveBattleOverlayDrawable)
            battleDecisionWidget.onRevealListener = { reveal ->
                if (reveal) {
                    extraActionLayout.hideItem(R.id.undo_button)
                    extraActionLayout.setTopOffset(3 * battleLogContainer.height / 5)
                } else {
                    extraActionLayout.showItem(R.id.undo_button)
                    extraActionLayout.setTopOffset(0, BattleDecisionWidget.REVEAL_ANIMATION_DURATION)
                }
            }


            extraActions.timerButton.setOnClickListener(this@BattleFragment)
            extraActions.forfeitButton.setOnClickListener(this@BattleFragment)
            extraActions.sendButton.setOnClickListener(this@BattleFragment)
            undoButton.setOnClickListener(this@BattleFragment)
            rematchButton.setOnClickListener(this@BattleFragment)
            uploadReplayButton.setOnClickListener(this@BattleFragment)

            replayActions.replayBackButton.setOnClickListener(this@BattleFragment)
            replayActions.replayPlayButton.setOnClickListener(this@BattleFragment)
            replayActions.replayForwardButton.setOnClickListener(this@BattleFragment)
        }
    }

    override fun onPause() {
        super.onPause()
        if (audioManager.isPlayingBattleMusic) {
            audioManager.pauseBattleMusic()
            wasPlayingBattleMusicWhenPaused = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (wasPlayingBattleMusicWhenPaused) {
            audioManager.playBattleMusic()
            wasPlayingBattleMusicWhenPaused = false
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        // Pause replay if user switches away to another fragment
        // Do a isResumed check, because this method gets triggered on activity start, and
        // battleType is not yet available at that point
        if (super.isResumed() && hidden && observer.isReplay) pauseReplay()
    }

    override fun onServiceBound(service: ShowdownService) {
        super.onServiceBound(service)
        service.battleMessageObserver.uiCallbacks = this
    }

    override fun onServiceWillUnbound(service: ShowdownService) {
        super.onServiceWillUnbound(service)
        service.battleMessageObserver.uiCallbacks = null
    }

    fun forfeit() = service?.sendRoomCommand(observedRoomId, "forfeit")

    fun sendDecision(reqId: Int, decision: BattleDecision) {
        service?.sendRoomCommand(observedRoomId, decision.command, decision.build(), reqId)
    }

    fun sendTimerCommand(on: Boolean) {
        service?.sendRoomCommand(observedRoomId, "timer", if (on) "on" else "off")
    }

    fun sendUndoCommand() = service?.sendRoomCommand(observedRoomId, "undo")

    fun sendSaveReplayCommand() = service?.sendRoomCommand(observedRoomId, "savereplay")

    private fun prepareBattleFieldUi() = binding.apply {
        battleLayout.alpha = 1f
        overlayImage.alpha = 1f
        overlayImage.setImageDrawable(null)
    }

    private fun clearBattleFieldUi(animate: Boolean = true) = binding.apply {
        val clearUiAction = Runnable {
            battleLayout.getSideView(Player.TRAINER).clearAllSides()
            battleLayout.getSideView(Player.FOE).clearAllSides()
            battleLayout.getStatusViews(Player.TRAINER).forEach { it.clear() }
            battleLayout.getPokemonViews(Player.TRAINER).forEach { it.setImageDrawable(null) }
            battleLayout.getStatusViews(Player.FOE).forEach { it.clear() }
            battleLayout.getPokemonViews(Player.FOE).forEach { it.setImageDrawable(null) }
            trainerInfo.clear()
            foeInfo.clear()
            battleMessage.apply {
                animate().cancel()
                alpha = 0f
            }
            if (overlayImage.drawable != inactiveBattleOverlayDrawable) {
                if (animate) {
                    overlayImage.setImageDrawable(null)
                    overlayImage.alpha = 0f
                    overlayImage.setImageDrawable(inactiveBattleOverlayDrawable)
                    overlayImage.animate().alpha(1f)
                } else {
                    overlayImage.setImageDrawable(inactiveBattleOverlayDrawable)
                }
            }
        }
        if (animate) battleLayout.animate().alpha(0f).withEndAction(clearUiAction).start()
        else clearUiAction.run()
    }

    override fun onClick(clickedView: View?) {
        if (observedRoomId == null) return
        when (clickedView) {
            binding.extraActions.forfeitButton -> {
                if (observer.isReplay) {
                    service?.replayManager?.closeReplay()
                } else if (battleRunning && observer.isUserPlaying) {
                    AlertDialog.Builder(requireActivity())
                            .setMessage("Do you really want to forfeit this battle ?")
                            .setPositiveButton("Forfeit") { _, _ -> forfeit() }
                            .setNegativeButton("Cancel", null)
                            .show()
                } else { // Acts as a leave button when user is spectator
                    service?.sendRoomCommand(observedRoomId, "leave")
                }
            }
            binding.extraActions.sendButton -> {
                val dialogView: View = layoutInflater.inflate(R.layout.dialog_battle_message, null)
                val editText = dialogView.findViewById<EditText>(R.id.edit_text_team_name)
                val dialog = MaterialAlertDialogBuilder(requireActivity())
                        .setPositiveButton("Send") { d, _ -> sendChatMessage(editText.text); d.dismiss() }
                        .setNegativeButton("Cancel", null)
                        .setNeutralButton("\"gg\"") { d, _ -> sendChatMessage("gg"); d.dismiss() }
                        .setView(dialogView)
                        .show()
                editText.requestFocus()
                editText.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        sendChatMessage(editText.text)
                        dialog.dismiss()
                        return@setOnEditorActionListener true
                    }
                    false
                }
            }
            binding.extraActions.timerButton -> {
                if (battleRunning) sendTimerCommand(!timerEnabled)
            }
            binding.undoButton -> {
                binding.undoButton.isEnabled = false
                sendUndoCommand()
                observer.reAskForRequest()
            }
            binding.rematchButton -> {
                homeFragment.challengeSomeone(observer.foeUsername)
            }
            binding.uploadReplayButton -> {
                binding.extraActionLayout.hideItem(R.id.upload_replay_button)
                sendSaveReplayCommand()
            }
            binding.replayActions.replayForwardButton -> {
                service?.replayManager?.goToNextTurn()
            }
            binding.replayActions.replayBackButton -> {
                service?.replayManager?.goToStart()
            }
            binding.replayActions.replayPlayButton -> {
                if (service?.replayManager?.isPaused == true) unpauseReplay() else pauseReplay()
            }
        }
    }

    private fun pauseReplay() {
        binding.replayActions.replayPlayButton.setImageResource(R.drawable.ic_replay_play)
        service?.replayManager?.pause()
    }

    private fun unpauseReplay() {
        binding.replayActions.replayPlayButton.setImageResource(R.drawable.ic_replay_pause)
        service?.replayManager?.play()
    }

    private fun sendChatMessage(msg: CharSequence) {
        val regex = "[{}:\",|\\[\\]]".toRegex()
        val escaped = msg.toString().replace(regex, "")
        if (escaped.isNotBlank()) service?.sendRoomMessage(observedRoomId, escaped)
    }

    private val onBindPopupListener = { anchorView: View, titleView: TextView, descView: TextView, placeHolderTop: ImageView, placeHolderBottom: ImageView ->
        when (val data = anchorView.getTag(R.id.battle_data_tag)) {
            is BattlingPokemon -> bindBattlingPokemonTipPopup(data, titleView, descView, placeHolderTop, placeHolderBottom)
            is Move -> bindMoveTipPopup(data, titleView, descView, placeHolderTop, placeHolderBottom)
            is SidePokemon -> bindSidePokemonPopup(data, titleView, descView, placeHolderTop, placeHolderBottom)
        }
    }

    private fun bindBattlingPokemonTipPopup(pokemon: BattlingPokemon, titleView: TextView,
                                            descView: TextView, placeHolderTop: ImageView,
                                            placeHolderBottom: ImageView) {
        placeHolderTop.setImageDrawable(null)
        placeHolderBottom.setImageDrawable(null)

        titleView.apply {
            text = pokemon.name concat " " concat pokemon.gender.small() concat " l." concat pokemon.level.toString()
        }

        descView.apply {
            text = if (pokemon.species != pokemon.name) "${pokemon.species}\n" else ""

            pokemon.condition?.let { condition ->
                append("HP: ".small())
                append("%.1f%% ".format(condition.health * 100).bold().color(healthColor(condition.health)))
                if (pokemon.trainer && observer.isUserPlaying) append("(${condition.hp}/${condition.maxHp}) ".small())
                condition.status?.let { append(it.toUpperCase().small().tag(statusColor(it))) }
                append("\n")
            }

            var ability: String? = null
            if (pokemon.trainer && lastDecisionRequest?.side != null) {
                val sidePokemon = lastDecisionRequest!!.side[pokemon.position]
                if (pokemon.transformSpecies == null) { // Ditto case
                    pokemon.statModifiers.apply {
                        append("Atk:".small() concat calcReadableStat("atk", sidePokemon.stats.atk) concat
                                " Def:".small() concat calcReadableStat("def", sidePokemon.stats.def) concat
                                " Spa:".small() concat calcReadableStat("spa", sidePokemon.stats.spa) concat
                                " Spd:".small() concat calcReadableStat("spd", sidePokemon.stats.spd) concat
                                " Spe:".small() concat calcReadableStat("spe", sidePokemon.stats.spe) concat
                                "\n")
                    }
                }
                append("Ability: ".small() concat sidePokemon.ability concat "\n")
                ability = sidePokemon.ability

                append("Item: ".small() concat sidePokemon.item.or("None"))
                fragmentScope.launch {
                    assetLoader.item(sidePokemon.item.toId())?.let { item ->
                        Utils.replace(descView.editableText, sidePokemon.item, item.name)
                    }
                }
            }

            fragmentScope.launch {
                val dexPokemon = assetLoader.dexPokemon(pokemon.species.toId())
                if (dexPokemon == null) {
                    append("No dex entry for ${pokemon.species}")
                    return@launch
                }
                placeHolderTop.setImageResource(Type.getResId(dexPokemon.firstType))
                dexPokemon.secondType?.let { placeHolderBottom.setImageResource(Type.getResId(it)) }

                if (pokemon.trainer && observer.isUserPlaying) {
                    if (ability == null) return@launch
                    val abilityName = if (dexPokemon.hiddenAbility?.toId() == ability) dexPokemon.hiddenAbility
                    else  dexPokemon.abilities.firstOrNull { it.toId() == ability }
                    if (abilityName != null) Utils.replace(descView.editableText, ability, abilityName)
                    return@launch
                }

                if (dexPokemon.abilities.isNotEmpty() && dexPokemon.hiddenAbility != null) {
                    append("Possible abilities: ".small())
                    append(dexPokemon.abilities.plus(dexPokemon.hiddenAbility).joinToString(", "))
                    append("\n")
                } else {
                    append("Ability: ".small())
                    append(dexPokemon.hiddenAbility ?: dexPokemon.abilities.firstOrNull() ?: "none")
                    append("\n")
                }
                val speedRange = Stats.calculateSpeedRange(pokemon.level, dexPokemon.baseStats.spe, "Random Battle", observer.gen)
                append("Speed: ".small() concat "${speedRange[0]} to ${speedRange[1]}" concat " (before items/abilities/modifiers)".small())
            }
        }
    }

    private fun bindMoveTipPopup(move: Move, titleView: TextView, descView: TextView, placeHolderTop: ImageView,
                                 placeHolderBottom: ImageView) {
        var moveName: String? = null
        if (move.maxflag) moveName = move.maxDetails?.name ?: move.maxMoveId
        if (move.zflag) moveName = move.zName
        if (moveName == null) moveName = move.name
        titleView.text = moveName
        descView.text = ""
        var priority = -20
        if (move.maxflag) priority = move.maxDetails?.priority ?: 0
        if (move.zflag) priority = move.zDetails?.priority ?: 0
        if (priority == -20) priority = move.details?.priority ?: 0
        when {
            priority > 1 -> descView.append("Nearly always moves first (" concat priority.toSignedString().italic() concat ")\n")
            priority <= -1 -> descView.append("Nearly always moves last (" concat priority.toSignedString().italic() concat ")\n")
            priority == 1 -> descView.append("Usually moves first (" concat priority.toSignedString().italic() concat ")\n")
        }

        var basePower = -1
        if (move.maxflag) basePower = move.details?.maxPower ?: 0
        if (move.zflag) basePower = move.zDetails?.basePower ?: move.details?.zPower ?: 0
        if (basePower == -1) basePower = move.details?.basePower ?: 0
        if (basePower > 0) descView.append("Base power: $basePower\n")
        var accuracy = -20
        if (move.maxflag) accuracy = move.maxDetails?.accuracy ?: 0
        if (move.zflag) accuracy = 0
        if (accuracy == -20) accuracy = move.details?.accuracy ?: 0
        if (accuracy != 0) {
            descView.append("Accuracy: ")
            if (accuracy == -1) descView.append("can't miss") else descView.append(accuracy.toString())
            descView.append("\n")
        }
        var desc: String? = null
        if (move.maxflag) desc = move.maxDetails?.desc ?: ""
        if (move.zflag) desc = move.zDetails?.desc ?: move.details?.zEffect?.let { "Z-Effect: $it" } ?: ""
        if (desc == null) desc = move.details?.desc ?: ""
        if (desc.isNotBlank()) descView.append(desc.italic())
        var type: String? = null
        if (move.maxflag) type = move.maxDetails?.type ?: "???"
        if (type == null) type = move.details?.type ?: "???"
        placeHolderTop.setImageResource(Type.getResId(type))
        val category = move.details?.category
        val drawable = if (category != null) CategoryDrawable(category) else null
        placeHolderBottom.setImageDrawable(drawable)
    }

    private fun bindSidePokemonPopup(pokemon: SidePokemon, titleView: TextView,
                                     descView: TextView, placeHolderTop: ImageView, placeHolderBottom: ImageView) {
        titleView.text = pokemon.name
        descView.apply {
            text = "HP: ".small() concat String.format("%.1f%% ", pokemon.condition.health * 100).bold().color(healthColor(pokemon.condition.health)) concat
                    "(" + pokemon.condition.hp + "/" + pokemon.condition.maxHp + ")"
            if (pokemon.condition.status != null)
                append(pokemon.condition.status!!.toUpperCase().small().tag(statusColor(pokemon.condition.status)))

            append("\n" concat
                    "Atk:".small() concat pokemon.stats.atk.toString() concat
                    " Def:".small() concat pokemon.stats.def.toString() concat
                    " Spa:".small() concat pokemon.stats.spa.toString() concat
                    " Spd:".small() concat pokemon.stats.spd.toString() concat
                    " Spe:".small() concat pokemon.stats.spe.toString() concat
                    "\n" concat
                    "Ability: ".small() concat pokemon.ability concat "\n" concat
                    "Item: ".small() concat pokemon.item.or("None") concat "\n" concat
                    "Moves: ".small())
        }
        fragmentScope.launch {
            assetLoader.movesDetails(*pokemon.moves.map { it.toId() }.toTypedArray()).forEachIndexed { index, details ->
                descView.append("\n\t")
                descView.append(details?.name ?: pokemon.moves[index])
            }
        }
        fragmentScope.launch {
            assetLoader.item(pokemon.item.toId())?.let { item ->
                Utils.replace(descView.editableText, pokemon.item, item.name)
            }
        }
        placeHolderTop.setImageDrawable(null)
        placeHolderBottom.setImageDrawable(null)
        fragmentScope.launch {
            assetLoader.dexPokemon(pokemon.species.toId())?.let {dexPokemon ->
                placeHolderTop.setImageResource(Type.getResId(dexPokemon.firstType))
                if (dexPokemon.secondType != null) placeHolderBottom.setImageResource(Type.getResId(dexPokemon.secondType))
                val abilityName = dexPokemon.matchingAbility(pokemon.ability, or = "")
                if (abilityName.isNotEmpty()) Utils.replace(descView.editableText, pokemon.ability, abilityName)
            }
        }
    }

    private fun notifyNewMessageReceived() {
        mainActivity.showBadge(id)
    }

    override fun onTimerEnabled(enabled: Boolean) {
        timerEnabled = enabled
        val color = ContextCompat.getColor(requireActivity(), if (enabled) R.color.secondary else R.color.onSurfaceBackground)
        binding.extraActions.timerButton.drawable.setTint(color)
    }

    override fun onPlayerInit(playerUsername: String, foeUsername: String) {
        binding.trainerInfo.setUsername(playerUsername)
        binding.foeInfo.setUsername(foeUsername)
        if (!observer.isUserPlaying) { // Spectator cannot forfeit nor toggle timer
            binding.extraActions.forfeitButton.setImageResource(R.drawable.ic_exit)
            binding.extraActions.timerButton.visibility = GONE
        }
    }

    override fun onBattleStarted() {
        prepareBattleFieldUi()
        mainActivity.setKeepScreenOn(true)
        when (observer.gameType) {
            GameType.SINGLE -> binding.battleLayout.setMode(BattleLayout.MODE_BATTLE_SINGLE)
            GameType.DOUBLE -> binding.battleLayout.setMode(BattleLayout.MODE_BATTLE_DOUBLE)
            GameType.TRIPLE -> binding.battleLayout.setMode(BattleLayout.MODE_BATTLE_TRIPLE)
        }
        if (soundEnabled) audioManager.playBattleMusic()
        //sendChatMessage("[Playing from the unofficial Android Showdown client]");
    }

    override fun onBattleEnded(winner: String) {
        mainActivity.setKeepScreenOn(false)
        audioManager.stopBattleMusic()
        inactiveBattleOverlayDrawable.setWinner(winner)
        clearBattleFieldUi()
        if (observer.isUserPlaying) {
            binding.extraActionLayout.apply {
                showItem(R.id.rematch_button)
                showItem(R.id.upload_replay_button)
            }
            binding.extraActions.forfeitButton.apply {
                setImageDrawable(ContextCompat.getDrawable(requireActivity(), R.drawable.ic_exit))
            }
            binding.extraActions.timerButton.visibility = GONE
        }
        if (observer.isReplay) {
            binding.replayActions.apply {
                replayPlayButton.isEnabled = false
                replayForwardButton.isEnabled = false
            }
        }
    }

    override fun onPreviewStarted() {
        if (isReplay) return // We skip team previewing for replays
        prepareBattleFieldUi()
        binding.battleLayout.setMode(BattleLayout.MODE_PREVIEW)
    }

    override fun onAddPreviewPokemon(id: PokemonId, pokemon: BasePokemon, hasItem: Boolean) {
        fragmentScope.launch {
            assetLoader.dexIcon(pokemon.species.toId())?.let {
                val infoView = if (!id.foe) binding.trainerInfo else binding.foeInfo
                infoView.appendPokemon(pokemon, BitmapDrawable(resources, it))
            }
        }
        if (isReplay) return // We skip team previewing for replays
        binding.battleLayout.getPokemonView(id)?.let {
            // Can be null when joining a battle where the preview has already been done
            glideHelper.loadPreviewSprite(id.player, pokemon, it)
        }
    }

    override fun onTeamSize(player: Player, size: Int) {
        val infoView = if (player == Player.TRAINER) binding.trainerInfo else binding.foeInfo
        infoView.setTeamSize(size)
        binding.battleLayout.setPreviewTeamSize(player, size)
    }

    override fun onFaint(id: PokemonId) {
        binding.battleLayout.getPokemonView(id)?.let {
            it.animate()
                    .setDuration(250)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .translationY(it.height / 2f)
                    .alpha(0f)
                    .withEndAction { it.translationY = 0f }
                    .start()
        }
        binding.battleLayout.getStatusView(id)?.animate()?.alpha(0f)?.start()
        val playerView = if (id.foe) binding.foeInfo else binding.trainerInfo
        playerView.setPokemonFainted(observer.getBattlingPokemon(id))
        if (soundEnabled) audioManager.playPokemonCry(observer.getBattlingPokemon(id), true)
    }

    override fun onMove(sourceId: PokemonId, targetId: PokemonId?, moveName: String, shouldAnim: Boolean) {
        if (!shouldAnim || targetId == null) return
        fragmentScope.launch {
            assetLoader.moveDetails(moveName)?.let { moveDetails ->
                val category = moveDetails.category.toId()
                if ("status" == category) return@let
                binding.battleLayout.displayHitIndicator(targetId)
            }
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    @SuppressLint("ClickableViewAccessibility")
    override fun onSwitch(pokemon: BattlingPokemon) {
        if (!pokemon.id.isInBattle) return
        binding.battleLayout.getStatusView(pokemon.id)?.apply {
            setPokemon(pokemon)
            animate().alpha(1f).start()
        }
        binding.battleLayout.getPokemonView(pokemon.id)?.apply {
            setTag(R.id.battle_data_tag, pokemon)
            battleTipPopup.addTippedView(this)
            glideHelper.loadBattleSprite(pokemon, this)
        }
        fragmentScope.launch {
            assetLoader.dexIcon(pokemon.species.toId())?.let {
                val infoView = if (!pokemon.foe) binding.trainerInfo else binding.foeInfo
                infoView.updatePokemon(pokemon, BitmapDrawable(resources, it))
            }
        }
        if (soundEnabled) audioManager.playPokemonCry(pokemon, false)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun onDetailsChanged(pokemon: BattlingPokemon) {
        binding.battleLayout.getPokemonView(pokemon.id)?.let {
            glideHelper.loadBattleSprite(pokemon, it)
        }
        fragmentScope.launch {
            assetLoader.dexIcon(pokemon.species.toId())?.let {
                val infoView = if (!pokemon.foe) binding.trainerInfo else binding.foeInfo
                infoView.updatePokemon(pokemon, BitmapDrawable(resources, it))
            }
        }
        if (soundEnabled && "mega" == pokemon.forme) audioManager.playPokemonCry(pokemon, false)
    }

    override fun onSwap(id: PokemonId, targetIndex: Int) {
        binding.battleLayout.swap(id, targetIndex)
    }

    override fun onHealthChanged(id: PokemonId, condition: Condition) {
        val statusView = binding.battleLayout.getStatusView(id)
        statusView?.setHealth(condition.health)
    }

    override fun onStatusChanged(id: PokemonId, status: String?) {
        val statusView = binding.battleLayout.getStatusView(id)
        statusView?.setStatus(status)
    }

    override fun onStatChanged(id: PokemonId) {
        val statModifiers = observer.getBattlingPokemon(id)!!.statModifiers
        val statusView = binding.battleLayout.getStatusView(id)
        statusView?.updateModifier(statModifiers)
    }

    private fun checkWillCrash(request: BattleDecisionRequest): Boolean {
        return try {
            request.count
            false
        } catch (e: IllegalStateException) {
            val msg = "A special bug has occurred, please use the bug report button on " +
                    "the home panel, and mention that this was an \"Unknown battle type\" issue. " +
                    "Don't forget to specify on which format you were playing, and if the battle " +
                    "has just started when this message appeared. " +
                    "Thank you!"
            onPrintText(msg.color(Colors.RED))
            true
        }
    }

    override fun onDecisionRequest(request: BattleDecisionRequest) {
        lastDecisionRequest = request
        if (checkWillCrash(request)) return
        if (request.shouldWait) return
        binding.battleDecisionWidget.promptDecision(observer, battleTipPopup, request) { decision ->
            sendDecision(request.id, decision)
        }
        var hideSwitch = true
        for (which in 0 until request.count) {
            if (!request.trapped(which)) hideSwitch = false
            val hideMoves = request.forceSwitch(which)
            val moves = request.getMoves(which)
            if (hideMoves || moves == null || moves.isEmpty()) continue
            fragmentScope.launch {
                assetLoader.movesDetails(*moves.map { it.id }.toTypedArray()).forEachIndexed { index, details ->
                    moves[index].details = details
                }
                binding.battleDecisionWidget.notifyDetailsUpdated()
            }
            fragmentScope.launch {
                val zMoves = moves.map { it.zName?.toId() ?: "" }
                if (!zMoves.all { it.isBlank() }) {
                    assetLoader.movesDetails(*zMoves.toTypedArray()).forEachIndexed { index, details ->
                        moves[index].zDetails = details
                    }
                    binding.battleDecisionWidget.notifyDetailsUpdated()
                }
            }
            fragmentScope.launch {
                val maxMoves = moves.map { it.maxMoveId?.toId() ?: "" }
                if (!maxMoves.all { it.isBlank() }) {
                    assetLoader.movesDetails(*maxMoves.toTypedArray()).forEachIndexed { index, details ->
                        moves[index].maxDetails = details
                    }
                    binding.battleDecisionWidget.notifyMaxDetailsUpdated()
                }
            }
        }
        if (request.teamPreview || !hideSwitch) {
            val team = request.side
            fragmentScope.launch {
                assetLoader.dexIcons(*team.map { it.species.toId() }.toTypedArray()).forEachIndexed { index, bitmap ->
                    team[index].icon = bitmap
                }
                binding.battleDecisionWidget.notifyDexIconsUpdated()
            }
        }
    }

    override fun onDisplayBattleToast(id: PokemonId, text: String, color: Int) {
        val toasterView = binding.battleLayout.getToasterView(id)
        toasterView?.makeToast(text, color)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun onFieldEffectChanged(name: String?) {
        val resId = FieldEffects.getDrawableResourceId(name)
        if (resId == binding.overlayImage.tag) return
        binding.overlayImage.apply {
            if (resId > 0) {
                alpha = 0f
                setImageResource(resId)
                tag = resId
                animate().alpha(0.75f).setDuration(250).withEndAction(null)
                        .start()
            } else {
                animate().alpha(0f).setDuration(250).withEndAction { setImageDrawable(null) }
                        .start()
            }
        }
    }

    override fun onSideChanged(player: Player, side: String, start: Boolean) {
        binding.battleLayout.getSideView(player).apply {
            if (start) sideStart(side) else sideEnd(side)
        }
    }

    override fun onVolatileStatusChanged(id: PokemonId, vStatus: String, start: Boolean) {
        binding.battleLayout.getStatusView(id)?.apply {
            if (start) addVolatileStatus(vStatus) else removeVolatileStatus(vStatus)
        }
    }

    override fun onMarkBreak() {
        binding.apply {
            battleDecisionWidget.dismiss()
            extraActionLayout.hideItem(R.id.undo_button)
        }
    }

    override fun onPrintText(text: CharSequence) {
        val fullScrolled = Utils.fullScrolled(binding.battleLogContainer)
        val l = binding.battleLog.length()
        if (l > 0) binding.battleLog.append("\n")
        binding.battleLog.append(text)
        notifyNewMessageReceived()
        if (fullScrolled) postFullScroll()
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun onPrintBattleMessage(text: CharSequence) {
        binding.battleMessage.text = text
        binding.battleMessage.animate().cancel()
        binding.battleMessage.alpha = 1f
        binding.battleMessage.animate()
                .setDuration(1000)
                .setStartDelay(750)
                .alpha(0f)
                .start()
    }

    override fun onPrintHtml(html: String) {
        val mark = Any()
        val l = binding.battleLog.length()
        binding.battleLog.append("\u200C")
        binding.battleLog.editableText.setSpan(mark, l, l + 1, Spanned.SPAN_MARK_MARK)
        Html.fromHtml(html,
                Html.FROM_HTML_MODE_COMPACT,
                glideHelper.getHtmlImageGetter(assetLoader, binding.battleLog.width),
                Callback { spanned: Spanned? ->
                    val at = binding.battleLog.editableText.getSpanStart(mark)
                    if (at == -1) return@Callback   // Check if text has been cleared
                    val fullScrolled = Utils.fullScrolled(binding.battleLogContainer)
                    binding.battleLog.editableText
                            .insert(at, "\n")
                            .insert(at + 1, spanned)
                    notifyNewMessageReceived()
                    if (fullScrolled) postFullScroll()
                })
    }

    override fun goToLatest() {
        postFullScroll()
    }

    private fun postFullScroll() {
        binding.battleLogContainer.post { binding.battleLogContainer.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onRoomTitleChanged(title: String) {
        // Ignored
    }

    override fun onUpdateUsers(users: List<String>) {
        // Ignored
    }

    override fun onRoomInit() {
        soundEnabled = Preferences.isBattleSoundEnabled(requireContext())
        lastDecisionRequest = null
        onTimerEnabled(false)
        binding.apply {
            battleLog.clearText()
            battleDecisionWidget.dismissNow()
            extraActionLayout.apply {
                hideItem(R.id.rematch_button)
                hideItem(R.id.upload_replay_button)
            }
            extraActions.apply {
                forfeitButton.setImageResource(R.drawable.ic_forfeit)
                timerButton.visibility = VISIBLE
                sendButton.visibility = VISIBLE
            }
            backgroundImage.animate().apply {
                duration = 100
                alpha(0f)
                withEndAction {
                    val resId = if (Math.random() > 0.5) R.drawable.battle_bg_1 else R.drawable.battle_bg_2
                    backgroundImage.setImageResource(resId)
                    backgroundImage.animate().alpha(1f).withEndAction(null).start()
                }
                start()
            }

            if (observer.isReplay) {
                extraActionLayout.showItem(R.id.replay_actions)
                extraActions.apply {
                    sendButton.visibility = GONE
                    timerButton.visibility = GONE
                    forfeitButton.setImageResource(R.drawable.ic_exit)
                }
                replayActions.apply {
                    replayPlayButton.setImageResource(R.drawable.ic_replay_pause)
                    replayPlayButton.isEnabled = true
                    replayForwardButton.isEnabled = true
                }
            } else {
                extraActionLayout.hideItem(R.id.replay_actions)
            }
        }
        // In case of corrupted battle stream make sure we stop music at the next one
        audioManager.stopBattleMusic()
    }

    override fun onRoomDeInit() {
        mainActivity.setKeepScreenOn(false)
        lastDecisionRequest = null
        inactiveBattleOverlayDrawable.setWinner(null)
        clearBattleFieldUi(animate = false)
        onTimerEnabled(false)
        binding.apply {
            battleLog.clearText()
            battleDecisionWidget.dismiss()
            extraActionLayout.apply {
                hideItem(R.id.rematch_button)
                hideItem(R.id.upload_replay_button)
                hideItem(R.id.undo_button)
                hideItem(R.id.replay_actions)
            }
            extraActions.apply {
                forfeitButton.setImageResource(R.drawable.ic_forfeit)
                timerButton.visibility = VISIBLE
                sendButton.visibility = VISIBLE
            }
        }
        audioManager.stopBattleMusic()
    }
}
