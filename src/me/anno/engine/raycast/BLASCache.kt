package me.anno.engine.raycast

import me.anno.cache.Promise
import me.anno.cache.CacheSection
import me.anno.ecs.components.mesh.Mesh
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod

object BLASCache : CacheSection<Mesh, Nothing>("BLASCache") {

    var disableBLASCache = false

    private var timeoutMillis = 1000L // timeout not really relevant 🤔
    private val generator = { mesh: Mesh, result: Promise<Nothing> ->
        if (mesh.raycaster == null) {
            mesh.raycaster = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)
        }
        result.value = null
    }

    fun getBLAS(mesh: Mesh): BLASNode? {
        if (disableBLASCache) return null
        val result = mesh.raycaster
        if (result != null) return result // quick-path
        getEntry(mesh, timeoutMillis, generator)
        return mesh.raycaster
    }
}