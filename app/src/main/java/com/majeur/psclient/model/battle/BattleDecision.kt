package com.majeur.psclient.model.battle


class BattleDecision {

    var command: String? = null
        private set

    private val choices = mutableListOf<Choice>()
    private var teamSize = 0

    private data class Choice(
            val action: String = "",
            val index: Int = 0,
            val extra: String? = null,
            var target: Int = 0
    )

    fun addSwitchChoice(who: Int) {
        command = CMD_CHOOSE
        choices.add(Choice(action = ACTION_SWITCH, index = who))
    }

    fun addMoveChoice(which: Int, mega: Boolean, zmove: Boolean, dynamax: Boolean) {
        command = CMD_CHOOSE
        val extra = if (mega) EXTRA_MEGA else if (zmove) EXTRA_ZMOVE else if (dynamax) EXTRA_DYNAMAX else null
        choices.add(Choice(action = ACTION_MOVE, index = which, extra = extra))
    }

    fun setLastMoveTarget(target: Int) {
        choices.last().target = target
    }

    fun addPassChoice() {
        command = CMD_CHOOSE
        choices.add(Choice(action = ACTION_PASS))
    }

    fun addLeadChoice(first: Int, teamSize: Int) {
        command = CMD_TEAM
        this.teamSize = teamSize
        choices.add(Choice(index = first))
    }

    fun leadChoicesCount() = choices.filter { it.action.isEmpty() }.size

    fun switchChoicesCount() = choices.filter { it.action == ACTION_SWITCH }.size

    fun hasSwitchChoice(which: Int) = choices.filter { it.action == ACTION_SWITCH && it.index == which }.isNotEmpty()

    fun hasOnlyPassChoice() = choices.all { it.action == ACTION_PASS }

    fun build(): String {
        return StringBuilder().run {
            if (command == CMD_TEAM) {
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