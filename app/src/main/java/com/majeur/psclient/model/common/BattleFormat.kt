package com.majeur.psclient.model.common

import com.majeur.psclient.util.toId
import java.io.Serializable

fun BattleFormat.toId(): String = this.label.toId()

class BattleFormat(val label: String, private val formatInt: Int) : Serializable {

    val isTeamNeeded: Boolean
        get() = formatInt and MASK_TEAM == 0

    val isSearchShow: Boolean
        get() = formatInt and MASK_SEARCH_SHOW == 0

    override fun equals(other: Any?) = (other as? BattleFormat)?.label == label

    override fun hashCode() = label.hashCode()

    class Category : Serializable {

        lateinit var label: String

        val formats = mutableListOf<BattleFormat>()

        val searchableBattleFormats: List<BattleFormat>
            get() = formats.filter { it.isSearchShow }
    }

    companion object {

        private const val MASK_TEAM = 0x1
        private const val MASK_SEARCH_SHOW = 0x2
        private const val MASK_CHALLENGE_SHOW = 0x4
        private const val MASK_TOURNAMENT_SHOW = 0x8

        @JvmStatic val FORMAT_OTHER = BattleFormat("[Other]", -1)

        fun compare(formats: List<Category>?, f1: String, f2: String): Int {
            if (f1 == f2) return 0
            if (f1.contains("other")) return 1
            if (f2.contains("other")) return -1
            if (formats == null) return f1.compareTo(f2)
            var f1Index = -1
            var f2Index = -1
            var index = 0
            loop@ for (category in formats) {
                for (format in category.formats) {
                    val id = format.toId()
                    if (id == f1) f1Index = index
                    if (id == f2) f2Index = index
                    if (f1Index >= 0 && f2Index >= 0) break@loop
                    index++
                }
            }
            return f1Index.compareTo(f2Index)
        }

        fun resolveName(formats: List<Category>?, formatId: String): String {
            if (formats == null) return formatId
            if ("other" == formatId) return "Other"
            for (category in formats) for (format in category.formats) if (format.toId().contains(formatId)) return format.label
            return formatId
        }
    }

}
