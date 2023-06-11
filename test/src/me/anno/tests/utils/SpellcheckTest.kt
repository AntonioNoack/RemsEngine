package me.anno.tests.utils

import me.anno.Engine
import me.anno.language.spellcheck.Spellchecking
import me.anno.utils.Sleep

fun main() {

    val answer = Sleep.waitUntilDefined(canBeKilled = false) {
        Spellchecking.check("I lov you", allowFirstLowercase = true)
    }

    println(answer)

    Engine.requestShutdown()

}