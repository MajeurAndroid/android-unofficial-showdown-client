package com.majeur.psclient.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.Property
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.children
import com.majeur.psclient.R
import com.majeur.psclient.model.battle.*
import com.majeur.psclient.model.battle.Move.Target.Companion.computeTargetAvailabilities
import com.majeur.psclient.model.pokemon.BattlingPokemon
import com.majeur.psclient.model.pokemon.SidePokemon
import com.majeur.psclient.service.observer.BattleRoomMessageObserver
import com.majeur.psclient.util.*
import java.util.*
import kotlin.math.hypot
import kotlin.math.roundToInt

class BattleActionWidget @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context!!, attrs, defStyleAttr), View.OnClickListener {

    var onRevealListener: ((Boolean) -> Unit)? = null

    private val moveButtons: MutableList<Button> = LinkedList()
    private val switchButtons: MutableList<SwitchButton> = LinkedList()
    private val movesCheckBox: CheckBox

    private val paint: Paint
    private var contentAlpha: Float = 1f
        set(a) { field = a; children.forEach { it.alpha = a }; paint.alpha = (255 * a).roundToInt(); invalidate() }
    private val alphaAnimator: ObjectAnimator
    private var revealAnimator: Animator? = null
    private var revealingIn = false
    private var revealingOut = false
    private var isAnimatingContentAlpha = false

    private var promptStage = 0
    private var targetToChoose: Move.Target? = null
    private var _observer: BattleRoomMessageObserver? = null
    private val observer get() = _observer!!
    private var _battleTipPopup: BattleTipPopup? = null
    private val battleTipPopup get() = _battleTipPopup!!
    private var _request: BattleActionRequest? = null
    private val request get() = _request!!
    private var _decision: BattleDecision? = null
    private val decision get() = _decision!!
    private var _onDecisionListener: ((BattleDecision) -> Unit)? = null
    private val onDecisionListener get() = _onDecisionListener!!

    init {
        visibility = View.GONE
        setWillNotDraw(false)
        paint = Paint().apply {
            color = ContextCompat.getColor(context!!, R.color.divider)
            strokeWidth = dp(1f).toFloat()
        }
        alphaAnimator = ObjectAnimator().apply {
            interpolator = DecelerateInterpolator()
            target = this@BattleActionWidget
            setProperty(CONTENT_ALPHA_PROPERTY)
        }
        repeat(4) {
            Button(context).apply {
                isAllCaps = false
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dp(6f), dp(6f), dp(6f), dp(6f))
                setOnClickListener(this@BattleActionWidget)
            }.also {
                addView(it)
                moveButtons.add(it)
            }
        }
        movesCheckBox = CheckBox(context).apply {
            buttonTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)), intArrayOf(Color.GRAY, Color.DKGRAY))
            addView(this, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        }
        val inflater = LayoutInflater.from(context)
        repeat(6) {
            (inflater.inflate(R.layout.button_switch, this, false) as SwitchButton).apply {
                setOnClickListener(this@BattleActionWidget)
            }.also {
                addView(it)
                switchButtons.add(it)
            }
        }
    }

    /* Layout methods */

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // TODO Handle and enforce Spec modes
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        var measuredHeight = paddingTop + paddingBottom
        val availableWidth = measuredWidth - paddingStart - paddingEnd
        var count = moveButtons.size
        if (count > 0) {
            val childWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth / count, MeasureSpec.EXACTLY)
            var maxChildHeight = 0
            var noVisibleChild = true
            for (i in 0 until count) {
                val child: View = moveButtons[i]
                child.measure(childWidthSpec, heightMeasureSpec)
                if (child.measuredHeight > maxChildHeight) maxChildHeight = child.measuredHeight
                if (child.visibility == View.VISIBLE) noVisibleChild = false
            }
            val childHeightSpec = MeasureSpec.makeMeasureSpec(maxChildHeight, MeasureSpec.EXACTLY)
            for (i in 0 until count) {
                moveButtons[i].measure(childWidthSpec, childHeightSpec)
            }
            if (!noVisibleChild) measuredHeight += maxChildHeight
        }
        count = switchButtons.size
        if (count > 0) {
            val childWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth / 3, MeasureSpec.EXACTLY)
            var noVisibleChild = true
            for (i in 0 until count) {
                val child: View = switchButtons[i]
                child.measure(childWidthSpec, heightMeasureSpec)
                if (child.visibility == View.VISIBLE) noVisibleChild = false
            }
            if (!noVisibleChild) {
                measuredHeight += switchButtons[0].measuredHeight
                if (count > 3) measuredHeight += switchButtons[3].measuredHeight
            }
        }
        val childWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST)
        movesCheckBox.measure(childWidthSpec, heightMeasureSpec)
        if (movesCheckBox.visibility == View.VISIBLE) measuredHeight += movesCheckBox.measuredHeight
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val paddingStart = paddingStart
        val paddingTop = paddingTop
        var yOffset = paddingTop
        var N = moveButtons.size
        var childWidth = width / N
        var childHeight = 0
        for (i in 0 until N) {
            val child: View = moveButtons[i]
            if (child.visibility == View.GONE) continue
            childHeight = child.measuredHeight
            child.layout(paddingStart + i * childWidth, yOffset,
                    paddingStart + (i + 1) * childWidth, yOffset + childHeight)
        }
        yOffset += childHeight
        if (movesCheckBox.visibility != View.GONE) {
            val checkBoxWidth = movesCheckBox.measuredWidth
            val checkBoxHeight = movesCheckBox.measuredHeight
            movesCheckBox.layout(width / 2 - checkBoxWidth / 2, yOffset,
                    width / 2 + checkBoxWidth / 2, yOffset + checkBoxHeight)
            yOffset += checkBoxHeight
        }
        N = switchButtons.size
        childWidth = width / (N / 2)
        for (i in 0..2) {
            val child: View = switchButtons[i]
            if (child.visibility == View.GONE) continue
            childHeight = child.measuredHeight
            child.layout(paddingStart + i * childWidth, yOffset,
                    paddingStart + (i + 1) * childWidth, yOffset + childHeight)
        }
        yOffset += childHeight
        var j: Int
        for (i in 3 until N) {
            val child: View = switchButtons[i]
            childHeight = child.measuredHeight
            j = i - 3
            child.layout(paddingStart + j * childWidth, yOffset,
                    paddingStart + (j + 1) * childWidth, yOffset + childHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (moveButtons.isNotEmpty()) {
            val offset = if (movesCheckBox.visibility != View.GONE) movesCheckBox.height else 0
            val child: View = moveButtons.first()
            if (child.visibility != View.GONE) canvas.drawLine(0f, offset + child.height.toFloat(),
                    width.toFloat(), offset + child.height.toFloat(), paint)
        }
    }

    /* Decision making methods */

    fun promptDecision(observer: BattleRoomMessageObserver, battleTipPopup: BattleTipPopup, request: BattleActionRequest,
                       listener: (BattleDecision) -> Unit) {
        promptStage = -1
        targetToChoose = null
        _observer = observer
        _battleTipPopup = battleTipPopup
        _request = request
        _onDecisionListener = listener
        _decision = BattleDecision()
        promptNext()
        revealIn()
    }

    private fun promptNext() {
        when {
            targetToChoose != null -> { // Needs target selection
                val targets = LinkedList<BattlingPokemon>()
                val foeTargets = LinkedList<BattlingPokemon>()
                for (i in 0 until request.count) {
                    observer.getBattlingPokemon(PokemonId(Player.TRAINER, i))?.let { targets.add(it) }
                    observer.getBattlingPokemon(PokemonId(Player.FOE, i))?.let { foeTargets.add(it) }
                }
                val arr = computeTargetAvailabilities(targetToChoose!!, promptStage, request.count)
                showTargetChoice(battleTipPopup, targets, foeTargets, arr)
            }
            if (request.teamPreview) promptStage == 0 else promptStage + 1 >= request.count -> { // Request completed
                onDecisionListener.invoke(decision)
                revealOut()
                promptStage = 0
                targetToChoose = null
                _observer = null
                _battleTipPopup = null
                _request = null
                _onDecisionListener = null
                _decision = null
            }
            else -> {
                promptStage += 1
                if (!request.teamPreview) {
                    val activeFainted = request.side[promptStage].condition.health == 0f
                    val unfaintedCount = request.side.drop(request.count).count { it.condition.health != 0f }
                    val switchChoicesCount = decision.switchChoicesCount()
                    val pass = activeFainted && unfaintedCount - switchChoicesCount <= 0
                    if (request.shouldPass(promptStage) || pass) {
                        decision.addPassChoice()
                        promptNext()
                        return
                    }
                }
                val hideMoves = request.forceSwitch(promptStage) || request.teamPreview
                val hideSwitch = request.trapped(promptStage)
                val moves = if (hideMoves) null else request.getMoves(promptStage)
                val team = if (hideSwitch) null else request.side
                showChoice(battleTipPopup,
                        moves,
                        request.canMegaEvo(promptStage),
                        request.canDynamax(promptStage),
                        request.isDynamaxed(promptStage),
                        team,
                        request.teamPreview)
            }
        }
    }

    private fun showTargetChoice(battleTipPopup: BattleTipPopup, trainerTargets: List<BattlingPokemon?>,
                                 foeTargets: List<BattlingPokemon?>, availabilities: Array<BooleanArray>) {
        alphaAnimator.apply {
            setFloatValues(1f, 0f)
            duration = ANIM_NEXTCHOICE_FADE_DURATION
            startDelay = 0
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = 1
            addListener(object : SimpleAnimatorListener() {
                override fun onAnimationStart(animator: Animator) {
                    isAnimatingContentAlpha = true
                }

                override fun onAnimationRepeat(animator: Animator) {
                    setTargetChoiceLayout(battleTipPopup, trainerTargets, foeTargets, availabilities)
                }

                override fun onAnimationEnd(animator: Animator) {
                    isAnimatingContentAlpha = false
                    alphaAnimator.removeListener(this)
                }
            })
        }.start()
    }

    private fun setTargetChoiceLayout(battleTipPopup: BattleTipPopup, trainerTargets: List<BattlingPokemon?>,
                                      foeTargets: List<BattlingPokemon?>, availabilities: Array<BooleanArray>) {
        moveButtons.forEach { btn ->
            btn.text = null
            btn.visibility = View.GONE
        }
        movesCheckBox.apply {
            visibility = View.GONE
            text = null
            setOnCheckedChangeListener(null)
        }
        for (i in 0 until 6) {
            val button = switchButtons[i]
            val targets = if (i < 3) foeTargets else trainerTargets
            val offset = if (i < 3) 0 else 3
            if (i - offset < targets.size) {
                val pokemon = targets[i - offset]
                button.visibility = View.VISIBLE
                button.setPokemonName(pokemon!!.name)
                val enabled = availabilities[if (i < 3) 0 else 1][i - offset] && pokemon.condition!!.health != 0f
                button.isEnabled = enabled
                button.setTag(R.id.battle_data_tag, pokemon)
                battleTipPopup.removeTippedView(button)
                button.setDexIcon(null)
            } else {
                button.setPokemonName(null)
                button.visibility = View.GONE
            }
        }
    }

    private fun showChoice(battleTipPopup: BattleTipPopup, moves: Array<Move>?, canMega: Boolean,
                           canDynamax: Boolean, isDynamaxed: Boolean, team: List<SidePokemon>?,
                           chooseLead: Boolean) {
        if (promptStage == 0 || decision.hasOnlyPassChoice()) { // First time a choice is shown, no need of transition
            setChoiceLayout(battleTipPopup, moves, canMega, canDynamax, isDynamaxed, team, chooseLead)
            return
        }
        alphaAnimator.apply {
            setFloatValues(1f, 0f)
            duration = ANIM_NEXTCHOICE_FADE_DURATION
            startDelay = 0
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = 1
            addListener(object : SimpleAnimatorListener() {
                override fun onAnimationStart(animator: Animator) {
                    isAnimatingContentAlpha = true
                }

                override fun onAnimationRepeat(animator: Animator) {
                    setChoiceLayout(battleTipPopup, moves, canMega, canDynamax, isDynamaxed, team, chooseLead)
                }

                override fun onAnimationEnd(animator: Animator) {
                    isAnimatingContentAlpha = false
                    alphaAnimator.removeListener(this)
                }
            })
        }.start()
    }

    private fun setChoiceLayout(battleTipPopup: BattleTipPopup, moves: Array<Move>?, canMega: Boolean, canDynamax: Boolean,
                                isDynamaxed: Boolean, team: List<SidePokemon>?, chooseLead: Boolean) {
        var canZMove = false
        moveButtons.forEachIndexed { i, btn ->
            if (moves != null && i < moves.size) {
                val move = moves[i]
                btn.apply {
                    text = moveText(move)
                    background.clearColorFilter()
                    visibility = View.VISIBLE
                    setTag(R.id.battle_data_tag, move)
                }
                battleTipPopup.addTippedView(btn)
                setMoveButtonEnabled(btn, !move.disabled)
                if (move.canZMove) canZMove = true
                if (move.details != null)
                    btn.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        move.details!!.color, BlendModeCompat.MODULATE)
            } else btn.apply {
                text = null
                visibility = View.GONE
            }
        }
        if (isDynamaxed) toggleMaxMoves(true)
        when {
            canMega -> movesCheckBox.apply {
                visibility = View.VISIBLE
                text = "Mega Evolution"
                isChecked = false
                setOnCheckedChangeListener(null)
            }
            canZMove -> movesCheckBox.apply {
                visibility = View.VISIBLE
                text = "Z-Move"
                isChecked = false
                setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean -> toggleZMoves(checked) }
            }
            canDynamax -> movesCheckBox.apply {
                visibility = View.VISIBLE
                text = "Dynamax"
                isChecked = false
                setOnCheckedChangeListener { _: CompoundButton?, checked: Boolean -> toggleMaxMoves(checked) }
            }
            else -> movesCheckBox.apply {
                visibility = View.GONE
                text = null
                setOnCheckedChangeListener(null)
            }
        }
        switchButtons.forEachIndexed { i, btn ->
            if (team != null && i < team.size) {
                val sidePokemon = team[i]
                val notInBattleAndUnfainted = i >= request.count && sidePokemon.condition.health != 0f
                val previouslyChosenAsSwitch = promptStage > 0 && request.count > 1 && decision.hasSwitchChoice(/* 1-based index */ i + 1)
                btn.apply {
                    visibility = View.VISIBLE
                    isEnabled = chooseLead || (notInBattleAndUnfainted && !previouslyChosenAsSwitch)
                    setPokemonName(sidePokemon.name)
                    setTag(R.id.battle_data_tag, sidePokemon)
                }
                battleTipPopup.addTippedView(btn)
                if (sidePokemon.icon != null) btn.setDexIcon(BitmapDrawable(resources, sidePokemon.icon))
            } else btn.apply {
                visibility = View.GONE
                setPokemonName(null)
                setDexIcon(null)
            }
        }
    }

    private fun toggleZMoves(toggle: Boolean) {
        moveButtons.forEach { button ->
            if (button.visibility == View.GONE) return@forEach
            val move = button.getTag(R.id.battle_data_tag) as Move
            if (toggle) {
                if (move.canZMove) {
                    button.text = move.zName
                    move.zflag = true
                    setMoveButtonEnabled(button, true)
                } else {
                    button.text = "—"
                    setMoveButtonEnabled(button, false)
                }
            } else {
                button.text = moveText(move)
                move.zflag = false
                setMoveButtonEnabled(button, !move.disabled)
            }
        }
    }

    private fun toggleMaxMoves(toggle: Boolean) {
        moveButtons.forEach { button ->
            if (button.visibility == View.GONE) return@forEach
            val move = button.getTag(R.id.battle_data_tag) as Move
            if (toggle) {
                if (move.maxMoveId != null) {
                    button.text = move.maxDetails?.name ?: move.maxMoveId!!
                    if (move.maxDetails != null) button.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                            move.maxDetails!!.color, BlendModeCompat.MODULATE) else button.background.clearColorFilter()
                    move.maxflag = true
                    setMoveButtonEnabled(button, true)
                } else {
                    button.text = "—"
                    setMoveButtonEnabled(button, false)
                }
            } else {
                button.text = moveText(move)
                if (move.details != null) button.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        move.details!!.color, BlendModeCompat.MODULATE) else button.background.clearColorFilter()
                setMoveButtonEnabled(button, !move.disabled)
                move.maxflag = false
            }
        }
    }

    private fun setMoveButtonEnabled(button: Button, enabled: Boolean) = button.apply {
        isEnabled = enabled
        setTextColor(if (enabled) Color.WHITE else Color.GRAY)
    }

    private fun moveText(move: Move) = move.name concat "\n" concat "${move.pp}/${move.ppMax}".small()

    fun notifyDexIconsUpdated() = switchButtons.forEach { btn ->
        val tag = btn.getTag(R.id.battle_data_tag)
        if (tag is SidePokemon) {
            val icon = tag.icon
            if (icon != null) btn.setDexIcon(BitmapDrawable(resources, icon))
        }
    }

    fun notifyDetailsUpdated() = moveButtons.forEach { btn ->
        val move = btn.getTag(R.id.battle_data_tag) as Move?
        if (move != null && !move.maxflag && move.details != null) {
            btn.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    move.details!!.color, BlendModeCompat.MODULATE)
        }
    }

    fun notifyMaxDetailsUpdated() = moveButtons.forEach { btn ->
        val move = btn.getTag(R.id.battle_data_tag) as Move?
        if (move != null && move.maxflag && move.maxDetails != null) {
            btn.text = move.maxDetails!!.name
            btn.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    move.maxDetails!!.color, BlendModeCompat.MODULATE)
        }
    }

    override fun onClick(view: View) {
        if (revealingIn || revealingOut || isAnimatingContentAlpha) return
        val data = view.getTag(R.id.battle_data_tag)
        if (data is Move) {
            val which = data.index + 1
            var mega = movesCheckBox.visibility == View.VISIBLE && movesCheckBox.isChecked
            var zmove = mega && data.zflag
            val dynamax = mega && data.maxflag
            if (dynamax) {
                zmove = false
                mega = zmove
            } else if (zmove) mega = false
            decision.addMoveChoice(which, mega, zmove, dynamax)
            if (request.count > 1 && data.target.isChoosable) targetToChoose = data.target
        } else if (data is BattlingPokemon) {
            val id = data.id
            var index = id.position + 1
            if (!id.foe) index *= -1
            decision.setLastMoveTarget(index)
            targetToChoose = null
        } else if (data is SidePokemon) {
            val who = data.index + 1
            if (request.teamPreview) {
                decision.addLeadChoice(who, request.side.size)
                view.isEnabled = false
                // Request full team order if one of our Pokémon has Illusion
                var fullTeamOrder = false
                for (pokemon in request.side) {
                    if (pokemon.baseAbility.toId() == "illusion") {
                        fullTeamOrder = true
                        break
                    }
                }
                if (decision.leadChoicesCount() < (if (fullTeamOrder) request.side.size else request.count)) return  // Avoid going to next prompt;
            } else {
                decision.addSwitchChoice(who)
            }
        }
        promptNext()
    }

    /* Reveal animation methods */

    private fun revealIn() {
        if (revealingIn || visibility == View.VISIBLE) return
        if (revealingOut) revealAnimator!!.cancel()
        val viewDiagonal = hypot(width.toDouble(), height.toDouble()).toInt()
        ViewAnimationUtils.createCircularReveal(this, 0, 0, 0f, viewDiagonal.toFloat())
        revealAnimator = ViewAnimationUtils.createCircularReveal(this, 0, 0, 0f, viewDiagonal.toFloat()).apply {
            startDelay = 0
            duration = ANIM_REVEAL_DURATION
            interpolator = AccelerateInterpolator()
            removeAllListeners()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    revealingIn = true
                    visibility = View.VISIBLE
                    contentAlpha = 0f
                }

                override fun onAnimationEnd(animation: Animator) {
                    revealingIn = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    revealingIn = false
                }
            })
            start()
        }
        alphaAnimator.apply {
            setFloatValues(0f, 1f)
            duration = ANIM_REVEAL_FADE_DURATION
            startDelay = ANIM_REVEAL_DURATION
            repeatCount = 0
        }.start()
        onRevealListener?.invoke(true)
    }

    private fun revealOut() {
        if (revealingOut || visibility == View.GONE) return
        if (revealingIn) revealAnimator!!.cancel()
        val viewDiagonal = hypot(width.toDouble(), height.toDouble()).toInt()
        revealAnimator = ViewAnimationUtils.createCircularReveal(this, 0, 0, viewDiagonal.toFloat(), 0f).apply {
            startDelay = ANIM_REVEAL_FADE_DURATION
            duration = ANIM_REVEAL_DURATION
            interpolator = AccelerateInterpolator()
            removeAllListeners()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    revealingOut = false
                    visibility = View.GONE
                }

                override fun onAnimationCancel(animation: Animator) {
                    revealingOut = false
                }
            })
            start()
        }
        alphaAnimator.apply {
            setFloatValues(1f, 0f)
            duration = ANIM_REVEAL_FADE_DURATION
            startDelay = 0
            repeatCount = 0
        }.start()

        // Setting the flag directly because reveal anim waits for fade anim to finish before running
        revealingOut = true
        onRevealListener?.invoke(false)
    }

    fun dismiss() {
        revealOut()
    }

    fun dismissNow() {
        visibility = View.GONE
        contentAlpha = 0f
        revealAnimator?.cancel()
    }

    companion object {

        private val CONTENT_ALPHA_PROPERTY = object : Property<BattleActionWidget, Float>(Float::class.java, "contentAlpha") {

            override fun get(widget: BattleActionWidget) = widget.contentAlpha

            override fun set(widget: BattleActionWidget, value: Float) {
                widget.contentAlpha = value
            }
        }

        private const val ANIM_REVEAL_DURATION = 250L
        private const val ANIM_REVEAL_FADE_DURATION = 100L
        private const val ANIM_NEXTCHOICE_FADE_DURATION = 225L
    }
}