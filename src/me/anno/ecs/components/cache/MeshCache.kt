package me.anno.ecs.components.cache

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshBaseComponent
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.LOGGER

object MeshCache : PrefabByFileCache<Mesh>(Mesh::class) {

    override operator fun get(ref: FileReference?, async: Boolean): Mesh? {
        if (ref == null || ref == InvalidRef) return null
        val pair = PrefabCache.getPrefabPair(ref, null, async)
        val instance = pair?.instance ?: return null
        return when (instance) {
            is Mesh -> instance
            is MeshComponent -> getMesh(instance, ref, async)
            is MeshBaseComponent -> instance.getMesh()
            is Entity -> {
                val comp = instance.getComponentInChildren(MeshBaseComponent::class, false)
                if (comp is MeshComponent) getMesh(comp, ref, async)
                else comp?.getMesh()
            }
            else -> {
                LOGGER.warn("Requesting mesh from ${instance.className}, cannot extract it")
                null
            }
        }
    }

    fun getMesh(instance: MeshComponent, ref: FileReference, async: Boolean): Mesh? {
        // warning: is there is a dependency ring, this will produce a stack overflow
        val ref2 = instance.mesh
        return if (ref == ref2) null
        else get(ref2, async)
    }

}