package me.anno.tests

import me.anno.ecs.components.script.ScriptComponent

fun main() {
    val globals = ScriptComponent.global.get()
    println(ScriptComponent.wrapIntoLimited(globals.load("while 1 > 0 do end"), globals).invoke())
}