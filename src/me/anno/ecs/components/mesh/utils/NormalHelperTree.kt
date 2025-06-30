package me.anno.ecs.components.mesh.utils

import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.types.Floats.toLongOr
import org.joml.AABBf
import org.joml.Vector3f
import kotlin.math.max

/**
 * todo: this can be cut down from 58ms to 34ms by using
 *  speiger.src.collections.longs.maps.impl.hash.Long2ObjectOpenHashMap
 * */
class NormalHelperTree(initialCapacity: Int, bounds: AABBf, minVertexDistance: Float) {

    companion object {
        private const val MAX_VALUE = 1L shl 21
        const val DX = 1L shl 42
        const val DY = 1L shl 21
        const val DZ = 1
    }

    // better than that isn't in our budget
    private val supportedMinVertexDistance = max(bounds.maxDelta / (MAX_VALUE - 5), minVertexDistance)
    private val scale = 1f / supportedMinVertexDistance

    private val x0 = bounds.minX - supportedMinVertexDistance * 2f
    private val y0 = bounds.minY - supportedMinVertexDistance * 2f
    private val z0 = bounds.minZ - supportedMinVertexDistance * 2f

    val entries = HashMap<Long, FloatArrayList>(initialCapacity)
    private val insertDelta = supportedMinVertexDistance * 0.5f

    fun hash(px: Float, py: Float, pz: Float): Long {
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

    fun hashLow(position: Vector3f): Long {
        return hash(position.x - insertDelta, position.y - insertDelta, position.z - insertDelta)
    }

    fun put(position: Vector3f, normal: Vector3f) {
        val hash = hash(position.x, position.y, position.z)
        val entry = entries.getOrPut(hash) { FloatArrayList(15) }
        if (entry.size < 32 * 3) entry.add(normal) // else limit reached
    }
}