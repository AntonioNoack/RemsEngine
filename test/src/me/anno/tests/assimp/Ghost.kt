package me.anno.tests.assimp

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.utils.OS

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    PrefabCache[OS.documents.getChild("CuteGhost.fbx")]!!
    Engine.requestShutdown()
}