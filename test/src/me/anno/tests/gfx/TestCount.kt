package me.anno.tests.gfx

import me.anno.Engine
import me.anno.ecs.Entity
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
    var sum = 0L
    entity.simpleTraversal {
        if (it is Entity) {
            it.anyComponent(MeshComponent::class) { mesh ->
                val mesh2 = MeshCache[mesh.meshFile]!!
                sum += mesh2.numPrimitives
                false
            }
        }
        false
    }
    LOGGER.debug("Primitives: $sum")
    Engine.requestShutdown()
}