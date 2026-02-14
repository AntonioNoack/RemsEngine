package me.anno.tests.mesh.fbx

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS

fun main() {
    OfficialExtensions.initForTests()
    val src = OS.downloads.getChild("3d/Talking On Phone.fbx")
    println(PrefabCache[src].waitFor()!!.sample)
    println(PrefabCache[src.getChild("FlatScene.json")].waitFor()!!.sample)
    Engine.requestShutdown()
}