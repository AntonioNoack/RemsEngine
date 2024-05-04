package me.anno.tests.assimp

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.io.files.Reference.getReference

fun main() {
    OfficialExtensions.initForTests()
    PrefabCache[getReference("res://meshes/CuteGhost.fbx")]!!
    Engine.requestShutdown()
}