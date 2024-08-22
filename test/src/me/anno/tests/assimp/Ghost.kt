package me.anno.tests.assimp

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.res

fun main() {
    OfficialExtensions.initForTests()
    PrefabCache[res.getChild("meshes/CuteGhost.fbx")]!!
    Engine.requestShutdown()
}