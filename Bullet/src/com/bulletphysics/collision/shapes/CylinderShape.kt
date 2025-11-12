package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.ecs.components.collider.Axis
import me.anno.maths.Maths.PIf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.hypot

/**
 * CylinderShape class implements a cylinder shape primitive, centered around
 * the origin. Its central axis aligned with the upAxis.
 *
 * @author jezek2
 */
open class CylinderShape(halfExtents: Vector3f, val upAxis: Axis) : BoxShape(halfExtents) {

    init {
        recalculateLocalAabb()
    }

    override fun getVolume(): Float {
        val boxVolume = super.getVolume()
        return boxVolume * PIf * 0.25f
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        getAabbBase(t, aabbMin, aabbMax)
    }

    private fun cylinderLocalSupport(halfExtents: Vector3f, v: Vector3f, out: Vector3f): Vector3f {

        val xAxis = upAxis.secondary
        val yAxis = upAxis.id
        val zAxis = upAxis.tertiary

        //mapping depends on how cylinder local orientation is
        // extents of the cylinder is: X,Y is for radius, and Z for height

        val radius = halfExtents[xAxis]
        val halfHeight = halfExtents[yAxis]

        val s = hypot(v[xAxis], v[zAxis])
        if (s != 0f) {
            val d = radius / s
            out[xAxis] = v[xAxis] * d
            out[yAxis] = if (v[yAxis] < 0f) -halfHeight else halfHeight
            out[zAxis] = v[zAxis] * d
        } else {
            out[xAxis] = radius
            out[yAxis] = if (v[yAxis] < 0f) -halfHeight else halfHeight
            out[zAxis] = 0f
        }
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f {
        val halfExtents = getHalfExtentsWithoutMargin(Stack.newVec3f())
        val result = cylinderLocalSupport(halfExtents, dir, out)
        Stack.subVec3f(1)
        return result
    }

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3f>,
        outs: Array<Vector3f>,
        numVectors: Int
    ) {
        val halfExtents = getHalfExtentsWithoutMargin(Stack.newVec3f())
        for (i in 0 until numVectors) {
            cylinderLocalSupport(halfExtents, dirs[i], outs[i])
        }
        Stack.subVec3f(1)
    }

    override fun localGetSupportingVertex(dir: Vector3f, out: Vector3f): Vector3f {
        localGetSupportingVertexWithoutMargin(dir, out)
        if (margin != 0f) {
            val norm = Stack.newVec3f(dir)
            if (norm.lengthSquared() < (BulletGlobals.SIMD_EPSILON * BulletGlobals.SIMD_EPSILON)) {
                norm.set(-1f, 0f, 0f)
            }
            norm.normalize()
            out.fma(margin, norm)
            Stack.subVec3f(1)
        }
        return out
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CYLINDER

    val radius: Float
        get() {
            val tmp = Stack.newVec3f()
            getHalfExtentsWithMargin(tmp)
            val r = if (upAxis != Axis.X) tmp.x else tmp.y
            Stack.subVec3f(1)
            return r
        }
}
