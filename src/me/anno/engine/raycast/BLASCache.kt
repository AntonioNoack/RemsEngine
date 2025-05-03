package me.anno.engine.raycast

import me.anno.cache.CacheSection
import me.anno.cache.NonDestroyingCacheData
import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map

object BLASCache : CacheSection("BLASCache") {

    var disableBLASCache = false

    private var timeoutMillis = 1000L // timeout not really relevant 🤔
    private val generator = { mesh: Mesh ->
        if (mesh.raycaster == null) {
            mesh.raycaster = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)
        }
        NonDestroyingCacheData(mesh)
    }

    fun getBLAS(mesh: Mesh): BLASNode? {
        if (disableBLASCache) return null
        val result = mesh.raycaster
        if (result != null) return result // quickpath
        return getEntry(mesh, timeoutMillis, true, generator)
            ?.value?.raycaster
    }

    fun getBLASAsync(mesh: Mesh, callback: Callback<BLASNode?>) {
        return getEntryAsync(
            mesh, timeoutMillis, true, generator,
            callback.map { it.value.raycaster })
    }
}