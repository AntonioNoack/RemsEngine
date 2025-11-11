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
import kotlin.math.max
import kotlin.math.min

/**
 *
 * */
class HeightMapShape : ConcaveShape() {

    enum class TrianglePattern {
        NORMAL,
        FLIPPED,
        ZIGZAG,
        DIAMOND
    }

    var width = 2 // > 1
    var length = 2 // > 1

    // FloatArray|ShortArray
    lateinit var heightData: Any

    var minHeight = 0.0
    var maxHeight = 1.0

    var heightScale = 1.0

    var upAxis = Axis.Y

    // var hdt = 0.1 // height type aka Floats/Shorts/...
    var pattern = TrianglePattern.NORMAL

    val localScaling = Vector3d(1.0)

    val localAabbMin = Vector3d()
    val localAabbMax = Vector3d()

    // center
    val localOrigin = Vector3d()

    fun defineBounds() {
        heightScale = (maxHeight - minHeight) / 65535.0
        when (upAxis) {
            Axis.X -> {
                localAabbMin.set(minHeight, 0.0, 0.0)
                localAabbMax.set(maxHeight, width.toDouble(), length.toDouble())
            }
            Axis.Y -> {
                localAabbMin.set(0.0, minHeight, 0.0)
                localAabbMax.set(width.toDouble(), maxHeight, length.toDouble())
            }
            Axis.Z -> {
                localAabbMin.set(0.0, 0.0, minHeight)
                localAabbMax.set(width.toDouble(), length.toDouble(), maxHeight)
            }
        }
        localAabbMax.add(localAabbMin, localOrigin).mul(0.5)
    }

    override fun getVolume(): Double {
        // terrain usually isn't moveable by physics (concave), so it shouldn't matter
        return 1e308
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val localMin = Stack.newVec()
        val localMax = Stack.newVec()

        localAabbMin.sub(localOrigin, localMin).mul(localScaling)
        localAabbMax.sub(localOrigin, localMax).mul(localScaling)

        AabbUtil.transformAabb(
            localMin, localMax,
            margin, t, aabbMin, aabbMax
        )

        Stack.subVec(2)
    }

    /**
     * This returns the "raw" (user's initial) height, not the actual height.
     * The actual height needs to be adjusted to be relative to the center of the heightfield's AABB.
     * */
    fun getRawHeightFieldValue(x: Int, y: Int): Double {
        // float wouldn't need heightScale
        val index = y * width + x
        val heightData = heightData
        return when (heightData) {
            is FloatArray -> heightData[index].toDouble()
            is ShortArray -> heightData[index].toInt().and(0xffff) * heightScale + minHeight
            else -> throw NotImplementedError()
        }
    }

    /**
     * returns the vertex in bullet-local coordinates
     * */
    private fun getVertex(x: Int, y: Int, dst: Vector3d) {
        assertTrue(x in 0 until width)
        assertTrue(y in 0 until length)
        val xf = x - width * 0.5
        val yf = y - length * 0.5
        val hf = getRawHeightFieldValue(x, y)
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
        val localAabbMin = aabbMin.div(localScaling, Stack.newVec())
        val localAabbMax = aabbMax.div(localScaling, Stack.newVec())

        // account for local origin
        localAabbMin.add(localOrigin)
        localAabbMax.add(localOrigin)

        var startX = 0
        var endX = width - 2 // -1 for max allowed index, -1 because of points vs quads
        var startJ = 0
        var endJ = length - 2

        when (upAxis) {
            Axis.X -> {
                startX = max(startX, localAabbMin.y.toIntOr())
                endX = min(endX, localAabbMax.y.toIntOr())
                startJ = max(startJ, localAabbMin.z.toIntOr())
                endJ = min(endJ, localAabbMax.z.toIntOr())
            }
            Axis.Y -> {
                startX = max(startX, localAabbMin.x.toIntOr())
                endX = min(endX, localAabbMax.x.toIntOr())
                startJ = max(startJ, localAabbMin.z.toIntOr())
                endJ = min(endJ, localAabbMax.z.toIntOr())
            }
            Axis.Z -> {
                startX = max(startX, localAabbMin.x.toIntOr())
                endX = min(endX, localAabbMax.x.toIntOr())
                startJ = max(startJ, localAabbMin.y.toIntOr())
                endJ = min(endJ, localAabbMax.y.toIntOr())
            }
        }

        val a = Stack.newVec()
        val b = Stack.newVec()
        val c = Stack.newVec()
        for (j in startJ..endJ) {
            for (x in startX..endX) {
                if (flipTriangle(x, j)) {
                    // first triangle
                    getVertex(x, j, a)
                    getVertex(x + 1, j, b)
                    getVertex(x + 1, j + 1, c)
                    callback.processTriangle(a, b, c, x, j)
                    // second triangle
                    // getVertex(x, j, vertex0) // stays the same, thanks to Danny Chapman
                    getVertex(x + 1, j + 1, b)
                    getVertex(x, j + 1, c)
                    callback.processTriangle(a, b, c, x, j)
                } else {
                    // first triangle
                    getVertex(x, j, a)
                    getVertex(x, j + 1, b)
                    getVertex(x + 1, j, c)
                    callback.processTriangle(a, b, c, x, j)
                    // second triangle
                    getVertex(x + 1, j, a)
                    // getVertex(x , j + 1, vertex1) // stays the same
                    getVertex(x + 1, j + 1, c)
                    callback.processTriangle(a, b, c, x, j)
                }
            }
        }

        Stack.subVec(5)
    }

    private fun flipTriangle(x: Int, j: Int): Boolean {
        return when (pattern) {
            TrianglePattern.NORMAL -> false
            TrianglePattern.FLIPPED -> true
            TrianglePattern.ZIGZAG -> (j and 1) == 0
            TrianglePattern.DIAMOND -> ((j + x) and 1) == 0
        }
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d {
        // moving concave objects not supported
        return inertia.set(0.0)
    }

    override fun getLocalScaling(out: Vector3d): Vector3d {
        return out.set(localScaling)
    }

    override fun setLocalScaling(scaling: Vector3d) {
        localScaling.set(scaling)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONCAVE_TERRAIN
}