package me.anno.tests.utils

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache.loadScenePrefab
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    loadScenePrefab(desktop.getChild("Scene.json")).getSampleInstance()
    Engine.requestShutdown()
}