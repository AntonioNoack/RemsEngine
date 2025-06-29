package me.anno.ecs.components.mesh.utils

import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.types.Floats.toLongOr
import org.joml.AABBf
import org.joml.Vector3f
import kotlin.math.max

class NormalHelperTree(initialCapacity: Int, bounds: AABBf, minVertexDistance: Float) {

    companion object {
        private const val MAX_VALUE = 1L shl 21
        private const val DX = 1L shl 42
        private const val DY = 1L shl 21
        private const val DZ = 1
    }

    // better than that isn't in our budget
    private val supportedMinVertexDistance = max(bounds.maxDelta / (MAX_VALUE - 5), minVertexDistance)
    private val scale = 1f / supportedMinVertexDistance

    private val x0 = bounds.minX - supportedMinVertexDistance * 2f
    private val y0 = bounds.minY - supportedMinVertexDistance * 2f
    private val z0 = bounds.minZ - supportedMinVertexDistance * 2f

    private val entries = HashMap<Long, FloatArrayList>(initialCapacity)
    private val insertDelta = supportedMinVertexDistance * 0.5f

    private fun hash(px: Float, py: Float, pz: Float): Long {
        val rx = (px - x0) * scale
        val ry = (py - y0) * scale
        val rz = (pz - z0) * scale
        val sx = rx.toLongOr()
        val sy = ry.toLongOr()
        val sz = rz.toLongOr()
        assertTrue(sx in 0 until MAX_VALUE - 1)
        assertTrue(sy in 0 until MAX_VALUE - 1)
        assertTrue(sz in 0 until MAX_VALUE - 1)
        return sx.shl(42) or sy.shl(21) or sz
    }

    fun get(position: Vector3f): FloatArrayList? {
        return entries[hash(position.x, position.y, position.z)]
    }

    fun put(position: Vector3f, normal: Vector3f) {
        val hash = hash(position.x - insertDelta, position.y - insertDelta, position.z - insertDelta)
        for (i in 0 until 8) {
            val dx = i and 1
            val dy = (i shr 1) and 1
            val dz = (i shr 2) and 1
            val hashI = hash + DX * dx + DY * dy + DZ * dz
            val entry = entries.getOrPut(hashI) { FloatArrayList(15) }
            if (entry.size < 32 * 3) entry.add(normal) // else limit reached
        }
    }
}