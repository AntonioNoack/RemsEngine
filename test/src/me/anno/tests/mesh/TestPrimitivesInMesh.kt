package me.anno.tests.mesh

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.sumComponentsInChildren
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.tests.LOGGER
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    val file = downloads.getChild("3d/blender_chan.glb")
    val obj = PrefabCache[file].waitFor()?.sample ?: throw IllegalStateException("Missing $file")
    val entity = obj as Entity
    val totalNumPrimitives = entity.sumComponentsInChildren(MeshComponent::class) { comp ->
        MeshCache.getEntry(comp.meshFile).waitFor()!!.numPrimitives
    }
    LOGGER.info("Primitives: $totalNumPrimitives")
    Engine.requestShutdown()
}