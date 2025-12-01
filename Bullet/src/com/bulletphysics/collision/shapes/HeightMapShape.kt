/*
Bullet Continuous Collision Detection and Physics Library
Copyright (c) 2003-2009 Erwin Coumans  http://bulletphysics.org

This software is provided 'as-is', without any express or implied warranty.
In no event will the authors be held liable for any damages arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it freely,
subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Floats.toIntOr
import org.joml.Vector3d
import org.joml.Vector3f
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import kotlin.math.max
import kotlin.math.min

class HeightMapShape : ConcaveShape() {

    enum class TrianglePattern {
        NORMAL,
        FLIPPED,
        ZIGZAG,
        DIAMOND;

        fun flipTriangle(x: Int, j: Int): Boolean {
            return when (this) {
                NORMAL -> false
                FLIPPED -> true
                ZIGZAG -> (j and 1) == 0
                DIAMOND -> ((j + x) and 1) == 0
            }
        }
    }

    var sizeX = 2 // > 1
    var sizeZ = 2 // > 1

    // FloatArray|ShortArray
    lateinit var heightData: Any

    var minHeight = 0.0
    var maxHeight = 1.0

    var heightScale = 1.0
    var heightOffset = 0.0
    var unsigned = false

    // var hdt = 0.1 // height type aka Floats/Shorts/...
    var pattern = TrianglePattern.NORMAL

    val localAabbMin = Vector3d()
    val localAabbMax = Vector3d()

    // center
    val localOrigin = Vector3d()

    fun defineBounds() {
        localAabbMin.set(0.0, minHeight, 0.0)
        localAabbMax.set(sizeX.toDouble(), maxHeight, sizeZ.toDouble())
        localAabbMax.add(localAabbMin, localOrigin).mul(0.5)
    }

    override fun getVolume(): Float {
        // terrain usually isn't moveable by physics (concave), so it shouldn't matter
        return 1e38f
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val localMin = Stack.newVec3f()
        val localMax = Stack.newVec3f()

        localAabbMin.sub(localOrigin, localMin).mul(localScaling)
        localAabbMax.sub(localOrigin, localMax).mul(localScaling)

        AabbUtil.transformAabb(
            localMin, localMax,
            margin, t, aabbMin, aabbMax
        )

        Stack.subVec3f(2)
    }

    /**
     * returns value without localScaling
     * */
    fun getHeightFieldValue(x: Int, z: Int): Double {
        val index = z * sizeX + x
        return when (val heightData = heightData) {
            // floats don't need a scale
            is FloatArray -> return heightData[index].toDouble()
            is FloatBuffer -> return heightData[index].toDouble()
            // integer types need it
            is ShortArray -> shortToValue(heightData[index])
            is ShortBuffer -> shortToValue(heightData[index])
            is IntArray -> intToValue(heightData[index])
            is IntBuffer -> intToValue(heightData[index])
            else -> throw NotImplementedError()
        } * heightScale + heightOffset
    }

    private fun shortToValue(v: Short): Long {
        return if (unsigned) v.toLong().and(0xffff) else v.toLong()
    }

    private fun intToValue(v: Int): Long {
        return if (unsigned) v.toLong().and(0xffff) else v.toLong()
    }

    /**
     * returns the vertex in bullet-local coordinates
     * */
    private fun getVertex(x: Int, hf: Double, z: Int, dst: Vector3d) {
        assertTrue(x in 0 until sizeX)
        assertTrue(z in 0 until sizeZ)
        val xf = x - sizeX * 0.5
        val yf = z - sizeZ * 0.5
        dst.set(xf, hf, yf).mul(localScaling)
    }

    /**
     * returns the vertex in bullet-local coordinates
     * */
    private fun getVertex(x: Int, z: Int, dst: Vector3d) {
        val hf = getHeightFieldValue(x, z)
        return getVertex(x, hf, z, dst)
    }

    /**
     * process all triangles within the provided axis-aligned bounding box
     * basic algorithm:
     * - convert input aabb to local coordinates (scale down and shift for local origin)
     * - convert input aabb to a range of heightfield grid points (quantize)
     * - iterate over all triangles in that subset of the grid
     */
    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        // -1 for max allowed index, -1 because of points vs quads
        val minX = max((aabbMin.x / localScaling.x + localOrigin.x).toIntOr(), 0)
        val maxX = min((aabbMax.x / localScaling.x + localOrigin.x).toIntOr(), sizeX - 2)
        val minZ = max((aabbMin.z / localScaling.z + localOrigin.z).toIntOr(), 0)
        val maxZ = min((aabbMax.z / localScaling.z + localOrigin.z).toIntOr(), sizeZ - 2)
        if (minX > maxX || minZ > maxZ) return

        val a = Stack.newVec3d()
        val b = Stack.newVec3d()
        val c = Stack.newVec3d()
        val d = Stack.newVec3d()

        val pattern = pattern
        for (z in minZ..maxZ) {
            getVertex(minX, z, c)
            getVertex(minX, z + 1, d)
            for (x in minX..maxX) {
                a.set(c); b.set(d)
                getVertex(x + 1, z, c)
                getVertex(x + 1, z + 1, d)

                val minY = min(min(a.y, b.y), min(c.y, d.y))
                val maxY = max(max(a.y, b.y), max(c.y, d.y))
                if (minY > aabbMax.y || maxY < aabbMin.y) continue // skip quads that are outside the bounds

                if (pattern.flipTriangle(x, z)) {
                    callback.processTriangle(a, c, d, x, z)
                    callback.processTriangle(a, d, b, x, z)
                } else {
                    callback.processTriangle(c, d, b, x, z)
                    callback.processTriangle(c, b, a, x, z)
                }
            }
        }

        Stack.subVec3d(4)
    }

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        // moving concave objects not supported
        return inertia.set(0f)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONCAVE_TERRAIN
}