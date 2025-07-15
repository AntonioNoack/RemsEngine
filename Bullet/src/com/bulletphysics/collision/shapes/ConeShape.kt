package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.BoxShape.Companion.boxInertia
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
        if (v[upAxisId] > v.length() * sinAngle) {
            out[secondary] = 0.0
            out[upAxisId] = halfHeight
            out[tertiary] = 0.0
        } else {
            val v0 = v[secondary]
            val v2 = v[tertiary]
            val s = hypot(v0, v2)
            if (s > BulletGlobals.FLT_EPSILON) {
                val d = radius / s
                out[secondary] = v0 * d
                out[upAxisId] = -halfHeight
                out[tertiary] = v2 * d
            } else {
                out[secondary] = 0.0
                out[upAxisId] = -halfHeight
                out[tertiary] = 0.0
            }
        }
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {
        return coneLocalSupport(dir, out)
    }

    override fun localGetSupportingVertex(dir: Vector3d, out: Vector3d): Vector3d {
        val supVertex = coneLocalSupport(dir, out)
        if (margin != 0.0) {
            val vecNorm = Stack.newVec(dir)
            if (vecNorm.lengthSquared() < BulletGlobals.FLT_EPSILON_SQ) {
                vecNorm.set(-1.0, -1.0, -1.0)
            }
            vecNorm.normalize()
            supVertex.fma(margin, vecNorm)
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
