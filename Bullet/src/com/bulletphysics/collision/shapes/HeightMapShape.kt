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
import me.anno.ecs.components.collider.Axis
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

    var sizeI = 2 // > 1
    var sizeJ = 2 // > 1

    // FloatArray|ShortArray
    lateinit var heightData: Any

    var minHeight = 0.0
    var maxHeight = 1.0

    var heightScale = 1.0
    var heightOffset = 0.0
    var unsigned = false

    var upAxis = Axis.Y

    // var hdt = 0.1 // height type aka Floats/Shorts/...
    var pattern = TrianglePattern.NORMAL

    val localAabbMin = Vector3d()
    val localAabbMax = Vector3d()

    // center
    val localOrigin = Vector3d()

    fun defineBounds() {
        when (upAxis) {
            Axis.X -> {
                localAabbMin.set(minHeight, 0.0, 0.0)
                localAabbMax.set(maxHeight, sizeI.toDouble(), sizeJ.toDouble())
            }
            Axis.Y -> {
                localAabbMin.set(0.0, minHeight, 0.0)
                localAabbMax.set(sizeI.toDouble(), maxHeight, sizeJ.toDouble())
            }
            Axis.Z -> {
                localAabbMin.set(0.0, 0.0, minHeight)
                localAabbMax.set(sizeI.toDouble(), sizeJ.toDouble(), maxHeight)
            }
        }
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
    fun getHeightFieldValue(x: Int, y: Int): Double {
        val index = y * sizeI + x
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
    private fun getVertex(x: Int, y: Int, dst: Vector3d) {
        assertTrue(x in 0 until sizeI)
        assertTrue(y in 0 until sizeJ)
        val xf = x - sizeI * 0.5
        val yf = y - sizeJ * 0.5
        val hf = getHeightFieldValue(x, y)
        when (upAxis) {
            Axis.X -> dst.set(hf, xf, yf)
            Axis.Y -> dst.set(xf, hf, yf)
            Axis.Z -> dst.set(xf, yf, hf)
        }
        dst.mul(localScaling)
    }

    /**
     * process all triangles within the provided axis-aligned bounding box
     * basic algorithm:
     * - convert input aabb to local coordinates (scale down and shift for local origin)
     * - convert input aabb to a range of heightfield grid points (quantize)
     * - iterate over all triangles in that subset of the grid
     */
    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d) {
        // scale down the input aabb's so they are in local (non-scaled) coordinates
        val localAabbMin = aabbMin.div(localScaling, Stack.newVec3d())
        val localAabbMax = aabbMax.div(localScaling, Stack.newVec3d())

        // account for local origin
        localAabbMin.add(localOrigin)
        localAabbMax.add(localOrigin)

        var minI = 0
        var maxI = sizeI - 2 // -1 for max allowed index, -1 because of points vs quads
        var minJ = 0
        var maxJ = sizeJ - 2

        when (upAxis) {
            Axis.X -> {
                minI = max(minI, localAabbMin.y.toIntOr())
                maxI = min(maxI, localAabbMax.y.toIntOr())
                minJ = max(minJ, localAabbMin.z.toIntOr())
                maxJ = min(maxJ, localAabbMax.z.toIntOr())
            }
            Axis.Y -> {
                minI = max(minI, localAabbMin.x.toIntOr())
                maxI = min(maxI, localAabbMax.x.toIntOr())
                minJ = max(minJ, localAabbMin.z.toIntOr())
                maxJ = min(maxJ, localAabbMax.z.toIntOr())
            }
            Axis.Z -> {
                minI = max(minI, localAabbMin.x.toIntOr())
                maxI = min(maxI, localAabbMax.x.toIntOr())
                minJ = max(minJ, localAabbMin.y.toIntOr())
                maxJ = min(maxJ, localAabbMax.y.toIntOr())
            }
        }

        val a = Stack.newVec3d()
        val b = Stack.newVec3d()
        val c = Stack.newVec3d()
        val d = Stack.newVec3d()
        val pattern = pattern
        for (j in minJ..maxJ) {
            for (i in minI..maxI) {
                getVertex(i, j, a)
                getVertex(i + 1, j, b)
                getVertex(i + 1, j + 1, c)
                getVertex(i, j + 1, d)
                if (pattern.flipTriangle(i, j)) {
                    callback.processTriangle(a, b, c, i, j)
                    callback.processTriangle(a, c, d, i, j)
                } else {
                    callback.processTriangle(b, c, d, i, j)
                    callback.processTriangle(b, d, a, i, j)
                }
            }
        }

        Stack.subVec3d(5)
    }

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        // moving concave objects not supported
        return inertia.set(0f)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONCAVE_TERRAIN
}