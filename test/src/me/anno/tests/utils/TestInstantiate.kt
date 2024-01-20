package me.anno.tests.utils

import me.anno.ecs.prefab.PrefabCache.loadScenePrefab
import me.anno.engine.ECSRegistry
import me.anno.utils.OS.desktop

fun main() {
    ECSRegistry.init()
    loadScenePrefab(desktop.getChild("Scene.json")).getSampleInstance()
}