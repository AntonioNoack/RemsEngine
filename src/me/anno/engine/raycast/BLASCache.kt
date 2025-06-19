package me.anno.engine.raycast

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map

object BLASCache : CacheSection<Mesh, Nothing>("BLASCache") {

    var disableBLASCache = false

    private var timeoutMillis = 1000L // timeout not really relevant 🤔
    private val generator = { mesh: Mesh, result: AsyncCacheData<Nothing> ->
        if (mesh.raycaster == null) {
            mesh.raycaster = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)
        }
        result.value = null
    }

    fun getBLAS(mesh: Mesh): BLASNode? {
        if (disableBLASCache) return null
        val result = mesh.raycaster
        if (result != null) return result // quickpath
        getEntry(mesh, timeoutMillis, true, generator)
        return mesh.raycaster
    }

    @Suppress("unused")
    fun getBLASAsync(mesh: Mesh, callback: Callback<BLASNode?>) {
        return getEntryAsync(
            mesh, timeoutMillis, true, generator,
            callback.map { mesh.raycaster })
    }
}