package me.anno.tests.assimp

import me.anno.ecs.prefab.PrefabCache
import me.anno.utils.OS

fun main() {
    val file = OS.documents.getChild("CuteGhost.fbx")
    PrefabCache[file]!!
}