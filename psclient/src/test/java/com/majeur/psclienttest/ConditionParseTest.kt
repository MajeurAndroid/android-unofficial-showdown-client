package com.majeur.psclienttest

import com.majeur.psclient.model.battle.Condition
import com.majeur.psclient.model.common.Colors.TYPE_ELECTRIC
import com.majeur.psclient.model.common.Colors.TYPE_FIRE
import com.majeur.psclient.model.common.Colors.TYPE_ICE
import com.majeur.psclient.model.common.Colors.TYPE_NORMAL
import com.majeur.psclient.model.common.Colors.TYPE_POISON
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ConditionParseTest {


    @Test
    fun `test_Parse normal condition`() {
        val rawCondition = "234/329"
        val condition = Condition(rawCondition)
        assert(condition.hp == 234) { "Wrong hp" }
        assert(condition.maxHp == 329) { "Wrong max hp" }
        assert(condition.status == null) { "Wrong status" }
        assert(condition.color == 0) { "Wrong status color" }
    }

    @Test
    fun `test_Parse conditions with status`() {
        listOf(TYPE_ELECTRIC, TYPE_FIRE, TYPE_NORMAL, TYPE_ICE, TYPE_POISON, TYPE_POISON)
            .zip(listOf("par", "brn", "slp", "frz", "tox", "psn")).forEach { (color, status) ->
                val rawCondition = "50/100 $status"
                val condition = Condition(rawCondition)
                assert(condition.hp == 50) { "Wrong hp" }
                assert(condition.maxHp == 100) { "Wrong max hp" }
                assert(condition.status == status) { "Wrong status" }
                assert(condition.color == color) { "Wrong status color" }
            }
    }

    @Test
    fun `test_Parse faint condition`() {
        val rawCondition = "0 fnt"
        val condition = Condition(rawCondition)
        assert(condition.hp == 0) { "Wrong hp" }
        assert(condition.health == 0f) { "Wrong health" }
        assert(condition.status == null) { "Wrong status" }
        assert(condition.color == 0) { "Wrong status color" }
    }

    @Test
    fun `test_Parse float normal condition`() {
        val rawCondition = "33.8"
        val condition = Condition(rawCondition)
        assert(condition.hp == 34) { "Wrong hp" }
        assert(condition.maxHp == 100) { "Wrong max hp" }
        assert(condition.status == null) { "Wrong status" }
        assert(condition.color == 0) { "Wrong status color" }
    }

    @Test
    fun `test_Parse float condition with status`() {
        val rawCondition = "33.8 slp"
        val condition = Condition(rawCondition)
        assert(condition.hp == 34) { "Wrong hp" }
        assert(condition.maxHp == 100) { "Wrong max hp" }
        assert(condition.status == "slp") { "Wrong status" }
        assert(condition.color == TYPE_NORMAL) { "Wrong status color" }
    }

    @Test
    fun `test_Parse float faint condition`() {
        val rawCondition = "0.0 fnt"
        val condition = Condition(rawCondition)
        assert(condition.hp == 0) { "Wrong hp" }
        assert(condition.health == 0f) { "Wrong max hp" }
        assert(condition.status == null) { "Wrong status" }
        assert(condition.color == 0) { "Wrong status color" }
    }

}