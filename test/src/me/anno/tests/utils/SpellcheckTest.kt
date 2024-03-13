package me.anno.tests.utils

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.language.spellcheck.Spellchecking
import me.anno.utils.Sleep

fun main() {

    OfficialExtensions.register()
    ExtensionLoader.load()

    val answer = Sleep.waitUntilDefined(canBeKilled = false) {
        Spellchecking.check("I lov you", allowFirstLowercase = true)
    }

    println(answer)

    Engine.requestShutdown()

}