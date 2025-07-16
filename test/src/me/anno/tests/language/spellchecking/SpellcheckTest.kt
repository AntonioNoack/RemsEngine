package me.anno.tests.language.spellchecking

import me.anno.engine.OfficialExtensions
import me.anno.language.spellcheck.Spellchecking
import me.anno.utils.assertions.assertContains
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class SpellcheckTest {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testSimpleSample() {
        OfficialExtensions.initForTests()
        val answers = Spellchecking.check("I lov you", allowFirstLowercase = true).waitFor()!!
        println(answers)
        assertEquals(1, answers.size)
        val answer = answers[0]
        assertEquals(2, answer.start)
        assertEquals(5, answer.end)
        assertContains("love", answer.improvements)
    }
}