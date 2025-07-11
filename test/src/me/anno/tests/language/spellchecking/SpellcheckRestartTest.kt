package me.anno.tests.language.spellchecking

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.language.spellcheck.Spellchecking
import org.junit.jupiter.api.Test

/**
 * Spellcheck has a special task restarting logic that needs testing.
 * */
class SpellcheckRestartTest {

    @Test
    fun testSpellcheckRestartingProcess() {
        OfficialExtensions.initForTests()
        request()
        Engine.requestShutdown()
        Thread.sleep(500)
        Engine.cancelShutdown()
        request()
    }

    fun request() {
        @Suppress("SpellCheckingInspection")
        Spellchecking.check("I am a setence.", true).waitFor()
    }
}