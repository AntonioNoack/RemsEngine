package me.anno.tests.lua

import me.anno.ecs.Entity
import me.anno.ecs.components.script.QuickInputScriptComponent
import me.anno.ecs.components.script.QuickScriptComponent

fun main() {

    val qis = QuickInputScriptComponent()
    val e = Entity()
    e.add(qis)
    qis.keyUpScript = "print(key)"
    qis.onKeyUp(17)

    val qs = QuickScriptComponent()
    e.add(qs)
    qs.createScript = "print(entity);print(17)"
    qs.onCreate()

}