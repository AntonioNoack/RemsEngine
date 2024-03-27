package me.anno.tests.bench

import me.anno.Build
import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.utils.Clock
import me.anno.utils.OS.downloads

fun main() {

    OfficialExtensions.initForTests()

    // 0.114s vs 0.629s for 10k instances, so .clone() is 6x faster than setting properties using reflections
    // -> usable slowdown :)
    Build.isShipped = true // 20% faster, because validation of duplicate names is skipped
    val clock = Clock()
    ECSRegistry.initMeshes()
    val prefab = PrefabCache[downloads.getChild("3d/azeria/scene.gltf")]!!
    clock.benchmark(50, 10000, "Prefab.clone") { // this is 7x faster, 11µs/instance
        prefab.createInstance() // calls sampleInstance.clone() internally
    }
    clock.benchmark(50, 10000, "Prefab.getSampleInstance()") {
        prefab.invalidateInstance()
        prefab.getSampleInstance() // has to create a new instance from CAdd+CSet, because we invalidated it
    }
    Engine.requestShutdown()
}