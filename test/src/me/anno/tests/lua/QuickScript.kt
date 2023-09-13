package me.anno.tests.lua

import me.anno.ecs.Entity
import me.anno.input.Key
import me.anno.lua.QuickInputScriptComponent
import me.anno.lua.QuickScriptComponent

fun main() {

    val qis = QuickInputScriptComponent()
    val e = Entity()
    e.add(qis)
    qis.keyUpScript = "print(key)"
    qis.onKeyUp(Key.KEY_ENTER)

    val qs = QuickScriptComponent()
    e.add(qs)
    qs.createScript = "print(entity);print(17)"
    qs.onCreate()

}