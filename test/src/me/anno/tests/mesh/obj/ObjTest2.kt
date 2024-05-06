package me.anno.tests.mesh.obj

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    PrefabCache[downloads.getChild("3d/bunny.obj")]!!
    Engine.requestShutdown()
}