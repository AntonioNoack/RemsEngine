package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.ecs.components.collider.Axis
import org.joml.Vector3d
import kotlin.math.hypot

/**
 * CylinderShape class implements a cylinder shape primitive, centered around
 * the origin. Its central axis aligned with the upAxis.
 *
 * @author jezek2
 */
open class CylinderShape(halfExtents: Vector3d, val upAxis: Axis) : BoxShape(halfExtents) {

    init {
        recalculateLocalAabb()
    }

    override fun getVolume(): Double {
        val boxVolume = super.getVolume()
        return boxVolume * Math.PI / 4.0
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        getAabbBase(t, aabbMin, aabbMax)
    }

    private fun cylinderLocalSupport(halfExtents: Vector3d, v: Vector3d, out: Vector3d): Vector3d {

        val xAxis = upAxis.secondary
        val yAxis = upAxis.id
        val zAxis = upAxis.tertiary

        //mapping depends on how cylinder local orientation is
        // extents of the cylinder is: X,Y is for radius, and Z for height

        val radius = halfExtents[xAxis]
        val halfHeight = halfExtents[yAxis]

        val s = hypot(v[xAxis], v[zAxis])
        if (s != 0.0) {
            val d = radius / s
            out[xAxis] = v[xAxis] * d
            out[yAxis] = if (v[yAxis] < 0.0) -halfHeight else halfHeight
            out[zAxis] = v[zAxis] * d
        } else {
            out[xAxis] = radius
            out[yAxis] = if (v[yAxis] < 0.0) -halfHeight else halfHeight
            out[zAxis] = 0.0
        }
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {
        val halfExtents = getHalfExtentsWithoutMargin(Stack.newVec())
        val result = cylinderLocalSupport(halfExtents, dir, out)
        Stack.subVec(1)
        return result
    }

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3d>,
        outs: Array<Vector3d>,
        numVectors: Int
    ) {
        val halfExtents = getHalfExtentsWithoutMargin(Stack.newVec())
        for (i in 0 until numVectors) {
            cylinderLocalSupport(halfExtents, dirs[i], outs[i])
        }
        Stack.subVec(1)
    }

    override fun localGetSupportingVertex(dir: Vector3d, out: Vector3d): Vector3d {
        localGetSupportingVertexWithoutMargin(dir, out)
        if (margin != 0.0) {
            val norm = Stack.newVec(dir)
            if (norm.lengthSquared() < (BulletGlobals.SIMD_EPSILON * BulletGlobals.SIMD_EPSILON)) {
                norm.set(-1.0)
            }
            norm.normalize()
            out.fma(margin, norm)
            Stack.subVec(1)
        }
        return out
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CYLINDER

    val radius: Double
        get() {
            val tmp = Stack.newVec()
            getHalfExtentsWithMargin(tmp)
            val r = if (upAxis != Axis.X) tmp.x else tmp.y
            Stack.subVec(1)
            return r
        }
}
