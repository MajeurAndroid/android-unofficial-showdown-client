package com.majeur.psclient.model.battle


class BattleDecision {

    private var _command: String? = null

    val command: String get() {
        if (_command == null) throw NullPointerException("BattleDecision has no command set")
        return _command!!
    }

    private val choices = mutableListOf<Choice>()
    private var teamSize = 0

    private data class Choice(
            val action: String = "",
            val index: Int = 0,
            val extra: String? = null,
            var target: Int = 0
    )

    /* 1 based index */
    fun addSwitchChoice(who: Int) {
        _command = CMD_CHOOSE
        choices.add(Choice(action = ACTION_SWITCH, index = who))
    }

    /* 1 based index */
    fun addMoveChoice(which: Int, mega: Boolean, zmove: Boolean, dynamax: Boolean) {
        _command = CMD_CHOOSE
        val extra = if (mega) EXTRA_MEGA else if (zmove) EXTRA_ZMOVE else if (dynamax) EXTRA_DYNAMAX else null
        choices.add(Choice(action = ACTION_MOVE, index = which, extra = extra))
    }

    /* 1 based index */
    fun setLastMoveTarget(target: Int) {
        choices.last().target = target
    }

    fun addPassChoice() {
        _command = CMD_CHOOSE
        choices.add(Choice(action = ACTION_PASS))
    }

    /* 1 based index */
    fun addLeadChoice(first: Int, teamSize: Int) {
        _command = CMD_TEAM
        this.teamSize = teamSize
        choices.add(Choice(index = first))
    }

    fun leadChoicesCount() = choices.count { it.action.isEmpty() }

    fun switchChoicesCount() = choices.count { it.action == ACTION_SWITCH }

    /* 1 based index */
    fun hasSwitchChoice(which: Int) = choices.any { it.action == ACTION_SWITCH && it.index == which }

    fun hasOnlyPassChoice() = choices.all { it.action == ACTION_PASS }

    fun build(): String {
        return StringBuilder().run {
            if (_command == CMD_TEAM) {
                choices.forEach { c -> append(c.index) }
                (1..teamSize).forEach { if (!contains(it.toString())) append(it) }
            } else {
                choices.forEach { c ->
                    append(c.action)
                    if (c.index != 0) append(" ").append(c.index)
                    if (c.extra != null) append(" ").append(c.extra)
                    if (c.target != 0) append(" ").append(c.target)
                    append(",")
                }
            }
            toString().removeSuffix(",")
        }
    }

    companion object {
        private const val CMD_CHOOSE = "choose"
        private const val CMD_TEAM = "team"
        private const val ACTION_MOVE = "move"
        private const val ACTION_SWITCH = "switch"
        private const val ACTION_PASS = "pass"
        private const val EXTRA_MEGA = "mega"
        private const val EXTRA_ZMOVE = "zmove"
        private const val EXTRA_DYNAMAX = "dynamax"
    }
}