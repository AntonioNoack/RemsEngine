package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.BoxShape.Companion.boxInertia
import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.setCoord
import com.bulletphysics.util.setScaleAdd
import cz.advel.stack.Stack
import me.anno.ecs.components.collider.Axis
import org.joml.Vector3d
import kotlin.math.hypot

/**
 * ConeShape implements a cone shape primitive, centered around the origin and
 * aligned with the upAxis.
 *
 * @author jezek2
 */
open class ConeShape(val radius: Double, val height: Double, val upAxis: Axis) : ConvexInternalShape() {

    private val sinAngle: Double = 1.0 / hypot(1.0, height / radius)

    val upAxisId get() = upAxis.id
    val secondary get() = upAxis.secondary
    val tertiary get() = upAxis.tertiary

    override fun getVolume(): Double {
        return radius * radius * height / 3.0
    }

    private fun coneLocalSupport(v: Vector3d, out: Vector3d): Vector3d {
        val halfHeight = height * 0.5
        if (getCoord(v, upAxisId) > v.length() * sinAngle) {
            setCoord(out, secondary, 0.0)
            setCoord(out, upAxisId, halfHeight)
            setCoord(out, tertiary, 0.0)
        } else {
            val v0 = getCoord(v, secondary)
            val v2 = getCoord(v, tertiary)
            val s = hypot(v0, v2)
            if (s > BulletGlobals.FLT_EPSILON) {
                val d = radius / s
                setCoord(out, secondary, getCoord(v, secondary) * d)
                setCoord(out, upAxisId, -halfHeight)
                setCoord(out, tertiary, getCoord(v, tertiary) * d)
            } else {
                setCoord(out, secondary, 0.0)
                setCoord(out, upAxisId, -halfHeight)
                setCoord(out, tertiary, 0.0)
            }
        }
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {
        return coneLocalSupport(dir, out)
    }

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3d>, outs: Array<Vector3d>, numVectors: Int
    ) {
        for (i in 0 until numVectors) {
            val vec = dirs[i]
            coneLocalSupport(vec, outs[i])
        }
    }

    override fun localGetSupportingVertex(dir: Vector3d, out: Vector3d): Vector3d {
        val supVertex = coneLocalSupport(dir, out)
        if (margin != 0.0) {
            val vecNorm = Stack.newVec(dir)
            if (vecNorm.lengthSquared() < (BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
                vecNorm.set(-1.0, -1.0, -1.0)
            }
            vecNorm.normalize()
            supVertex.setScaleAdd(margin, vecNorm, supVertex)
            Stack.subVec(1)
        }
        return supVertex
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONE_SHAPE_PROXYTYPE

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d {
        // todo surely, there's a better formula than just the box inertia...
        // also, should the margin be part of this? -> yes, better that way
        val margin = margin
        boxInertia(radius + margin, radius + margin, height * 0.5 + margin, mass, inertia)
        return inertia
    }
}
