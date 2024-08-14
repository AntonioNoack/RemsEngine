package me.anno.tests.utils

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.language.spellcheck.Spellchecking
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertContains
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class SpellcheckTest {
    @Test
    fun testSimpleSample() {
        OfficialExtensions.initForTests()

        val answers = Sleep.waitUntilDefined(canBeKilled = false) {
            Spellchecking.check("I lov you", allowFirstLowercase = true)
        }

        println(answers)
        assertEquals(1, answers.size)
        val answer = answers[0]
        assertEquals(2, answer.start)
        assertEquals(5, answer.end)
        assertContains("love", answer.improvements)
        Engine.requestShutdown()
    }
}