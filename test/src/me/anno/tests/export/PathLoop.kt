package me.anno.tests.export

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.desktop

fun main() {
    // there was an invalid path (infinite) when exporting C:/Users/Antonio/Downloads/3d/-_cat_girl_-_ffiv.glb
    OfficialExtensions.initForTests()
    val file = PrefabCache[desktop.getChild("TestGame.jar/res/0.json")].waitFor()
    file!!.newInstance()
    Engine.requestShutdown()
}