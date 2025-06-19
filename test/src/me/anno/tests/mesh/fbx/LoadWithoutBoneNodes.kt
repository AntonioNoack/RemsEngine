package me.anno.tests.mesh.fbx

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS

fun main() {
    OfficialExtensions.initForTests()
    val src = OS.downloads.getChild("3d/Talking On Phone.fbx")
    println(PrefabCache.getPrefabSampleInstance(src).waitFor()!!)
    println(PrefabCache.getPrefabSampleInstance(src.getChild("FlatScene.json")).waitFor()!!)
    Engine.requestShutdown()
}