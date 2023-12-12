package me.anno.tests.gfx

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.sumComponentsInChildren
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.LOGGER
import me.anno.utils.OS.downloads

fun main() {
    ECSRegistry.initMeshes()
    val file = getReference(downloads, "3d/blender_chan.glb")
    val obj = PrefabCache[file] ?: throw java.lang.IllegalStateException("Missing $file")
    val entity = obj.getSampleInstance() as Entity
    val totalNumPrimitives = entity.sumComponentsInChildren(MeshComponent::class) { comp ->
        MeshCache[comp.meshFile]!!.numPrimitives
    }
    LOGGER.debug("Primitives: $totalNumPrimitives")
    Engine.requestShutdown()
}