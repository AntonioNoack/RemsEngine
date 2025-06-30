package me.anno.ecs.components.mesh.utils

import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Floats.toLongOr
import org.joml.AABBf
import org.joml.Vector3f
import speiger.primitivecollections.LongToObjectHashMap
import kotlin.math.max

/**
 * todo: this class can be reused for our convex hull algorithm
 * */
open class HashVertexLookup<V>(initialCapacity: Int, bounds: AABBf, minVertexDistance: Float) {

    companion object {
        private const val MAX_VALUE = 1L shl 21
        private const val SAFETY_MARGIN = 5
        private const val INV_MAX_VALUE = 1f / (MAX_VALUE - SAFETY_MARGIN)
    }

    val entries = LongToObjectHashMap<V>(initialCapacity)

    private val scale: Float
    private val x0: Float
    private val y0: Float
    private val z0: Float
    private val roundingDelta: Float

    init {
        // better than that isn't in our budget
        val supportedMinVertexDistance = max(bounds.maxDelta * INV_MAX_VALUE, minVertexDistance)
        scale = 1f / supportedMinVertexDistance

        x0 = bounds.minX - supportedMinVertexDistance
        y0 = bounds.minY - supportedMinVertexDistance
        z0 = bounds.minZ - supportedMinVertexDistance

        roundingDelta = supportedMinVertexDistance * 0.5f
    }

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

    fun hash(position: Vector3f): Long {
        return hash(position.x, position.y, position.z)
    }

    /**
     * Returns the hash of the floored corner.
     * */
    fun hash0(position: Vector3f): Long {
        return hash(position.x - roundingDelta, position.y - roundingDelta, position.z - roundingDelta)
    }

    /**
     * Given the hash of the floored corner, and i from 0 to 7 (inclusive),
     * return the hash of the i-th corner in a 2x2x2 cube.
     *
     * Ideally, to keep allocations low, you insert into hash(), and query all corners using hash0() and hash8().
     * As an alternative, when there is much fewer puts than gets, use hash0() and hash8() to insert the value into all corners,
     * and just query using hash().
     * */
    fun hash8(hash0: Long, i: Int): Long {
        val dx = (i and 4).toLong()
        val dy = (i and 2).toLong()
        val dz = (i and 1).toLong()
        return hash0 +
                dx.shl(40) + // 2 bits left by i
                dy.shl(20) + // 1 bit  left by i
                dz // 0 bits left by i
    }
}