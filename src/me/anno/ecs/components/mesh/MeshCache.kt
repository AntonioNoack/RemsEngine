package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.utils.StaticMeshJoiner.findMeshes
import me.anno.ecs.components.mesh.utils.StaticMeshJoiner.joinMeshes
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.io.files.FileReference
import me.anno.io.saveable.Saveable
import org.apache.logging.log4j.LogManager

/**
 * Caches meshes. If given an Entity, MeshComponentBase or MeshSpawner, will join the meshes inside into a new mesh.
 * */
object MeshCache : PrefabByFileCache<IMesh>(IMesh::class, "Mesh") {

    private val LOGGER = LogManager.getLogger(MeshCache::class)

    /**
     * transforms any Saveable into a Mesh (as far as supported);
     * can be really expensive, so you should cache your results
     * */
    override fun castInstance(instance: Saveable?, ref: FileReference): IMesh? {
        return when (instance) {
            is IMesh -> instance
            is MeshComponentBase -> instance.getMesh()
            is MeshSpawner -> joinMeshes(listOf(instance))
            is Entity -> joinMeshes(findMeshes(instance))
            // cast Material?
            null -> null
            else -> {
                LOGGER.warn("Requesting mesh from ${instance.className}, cannot extract it")
                null
            }
        }
    }
}