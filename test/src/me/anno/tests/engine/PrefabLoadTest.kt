package me.anno.tests.engine

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    val src = desktop.getChild("0.json")
    val prefab = PrefabCache[src, false]!!
    println(prefab)
    Engine.requestShutdown()
}