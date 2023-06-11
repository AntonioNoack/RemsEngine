package me.anno.tests.utils

import me.anno.ecs.prefab.PrefabCache.loadScenePrefab
import me.anno.engine.ECSRegistry
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS.desktop

fun main() {
    ECSRegistry.init()
    loadScenePrefab(getReference(desktop, "Scene.json")).getSampleInstance()
}