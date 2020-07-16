package com.majeur.psclient.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import com.majeur.psclient.R
import com.majeur.psclient.model.common.BattleFormat
import com.majeur.psclient.model.common.BattleFormat.Companion.resolveName
import com.majeur.psclient.ui.MainActivity
import com.majeur.psclient.util.*

class PrivateMessagesOverviewWidget @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr), View.OnClickListener {

    var onItemClickListener: OnItemClickListener? = null
    var onItemButtonClickListener: OnItemButtonClickListener? = null
    val isEmpty: Boolean
        get() = childCount == 0

    private val colorSecondary by lazy { ContextCompat.getColor(context!!, R.color.secondary) }
    private val layoutInflater by lazy { LayoutInflater.from(context) }
    private val entries: List<Entry>
        get() = children.map { it.tag as Entry }.toList()

    init {
        orientation = VERTICAL
    }

    fun incrementPmCount(with: String) = getEntryOrCreate(with).run {
        pmCount += 1
        updateViewForEntry(this)
    }

    fun updateChallengeTo(to: String?, format: String?) = entries.forEach { entry ->
        if (entry.with == to) { // We are currently challenging this user
            entry.challengeFormat = format
            entry.challengeFromMe = true
            updateViewForEntry(entry)
        } else if (entry.challengeFromMe) { // We are not challenging this user anymore
            entry.challengeFormat = null
            entry.challengeFromMe = false
            updateViewForEntry(entry)
        }
    }

    fun updateChallengesFrom(users: Collection<String>, formats: Collection<String>) {
        if (users.isEmpty()) { // There is nobody challenging us
            entries.filter { it.challengeFormat != null && !it.challengeFromMe }.forEach { entry ->
                entry.challengeFormat = null
                if (entry.pmCount > 0) updateViewForEntry(entry) else removeViewForEntry(entry)
            }
            return
        }
        users.zip(formats).forEach { (user, format) -> // Adding or updating entries for user challenging us
            getEntryOrCreate(user).apply {
                challengeFormat = format
                challengeFromMe = false
                updateViewForEntry(this)
            }
        }
        entries.forEach { entry -> // Resetting (or removing) entries that are not challenging us anymore
            if (!entry.challengeFromMe && !users.contains(entry.with)) {
                entry.challengeFormat = null
                entry.challengeFromMe = false
                if (entry.pmCount > 0) updateViewForEntry(entry) else removeViewForEntry(entry)
            }
        }
    }

    private fun getEntryOrCreate(with: String) = entries.firstOrNull { it.with == with } ?: Entry(with).also {
        val view = layoutInflater.inflate(R.layout.list_item_pmentry, this) as ViewGroup
        view.setOnClickListener(this@PrivateMessagesOverviewWidget)
        view.findViewById<View>(R.id.button_challenge).setOnClickListener(this@PrivateMessagesOverviewWidget)
        view.tag = it
    }

    private fun removeViewForEntry(entry: Entry) = children.firstOrNull { it.tag == entry }?.let { removeView(it) }

    private fun updateViewForEntry(entry: Entry) {
        val view = children.first { it.tag == entry } as ViewGroup
        val label = view.findViewById<TextView>(R.id.label)
        label.text = entry.with.bold()
        val button = view.findViewById<MaterialButton>(R.id.button_challenge)
        button.text = "Challenge" // Default behaviour: challenging a user we are private chatting with
        button.isEnabled = true
        val hasChallenge = entry.challengeFormat != null
        if (hasChallenge) { // Second behaviour: waiting for response or send our response to a challenge request
            if (entry.challengeFromMe) { // Already challenging this user so button should be disabled
                button.isEnabled = false
            } else { // This user is challenging us
                button.text = "Accept"
                button.isEnabled = true
                label.append(" is challenging you !".color(colorSecondary).small() concat
                        "\n" concat resolveFormat(entry.challengeFormat!!).small())
            }
        }
        if (entry.pmCount > 0) { // If we have pms show this in view
            label.append("\n" concat "${entry.pmCount} message(s)".small().italic())
        }
    }

    private fun resolveFormat(format: String): String { // This can be done in a nicer way
        val activity = context as MainActivity
        val formats = activity.service!!.getSharedData<List<BattleFormat.Category>>("formats")
        return if (formats != null) resolveName(formats, format) else format
    }

    override fun onClick(v: View) {
        if (v is MaterialButton) { // Challenge btn
            val entry = (v.getParent() as View).tag as Entry
            if (entry.challengeFormat != null && !entry.challengeFromMe) {
                onItemButtonClickListener?.onAcceptButtonClick(entry.with, entry.challengeFormat!!)
            } else {
                onItemButtonClickListener?.onChallengeButtonClick(entry.with)
            }
        } else {
            val entry = v.tag as Entry
            onItemClickListener?.onItemClick(entry.with)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(with: String)
    }

    interface OnItemButtonClickListener {
        fun onChallengeButtonClick(with: String)
        fun onAcceptButtonClick(with: String, format: String)
    }

    private class Entry(var with: String) {
        var pmCount = 0 // Private messages count with this user
        var challengeFormat: String? = null
        // If challengeFormat is not null, challengeFromMe means that I am challenging 'with', else 'with' is challenging me
        var challengeFromMe = false

    }
}