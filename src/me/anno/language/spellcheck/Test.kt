package me.anno.language.spellcheck

import me.anno.utils.LOGGER

fun main() {

    Spellchecking.check("I love you", true, 0)

    var answer: List<*>? = null
    while (answer == null) {
        answer = Spellchecking.check("I love you", true, 0)
        Thread.sleep(10)
    }

    LOGGER.info(answer)

}