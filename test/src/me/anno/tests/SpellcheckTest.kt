package me.anno.tests

import me.anno.language.spellcheck.Spellchecking
import org.apache.logging.log4j.LogManager

fun main() {

    Spellchecking.check("I love you", true)

    var answer: List<*>? = null
    while (answer == null) {
        answer = Spellchecking.check("I love you", true)
        Thread.sleep(10)
    }

    LogManager.getLogger("SpellcheckingTest").info(answer)

}