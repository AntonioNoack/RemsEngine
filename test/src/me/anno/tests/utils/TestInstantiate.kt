package me.anno.tests.utils

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache.loadScenePrefab
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    ECSRegistry.init()
    loadScenePrefab(desktop.getChild("Scene.json")).getSampleInstance()
    Engine.requestShutdown()
}