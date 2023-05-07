package me.anno.tests.lua

import me.anno.lua.ScriptComponent

fun main() {
    val globals = ScriptComponent.global.get()
    println(ScriptComponent.wrapIntoLimited(globals.load("while 1 > 0 do end"), globals).invoke())
}