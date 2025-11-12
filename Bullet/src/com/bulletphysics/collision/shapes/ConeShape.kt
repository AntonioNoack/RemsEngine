package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.shapes.BoxShape.Companion.boxInertia
import cz.advel.stack.Stack
import me.anno.ecs.components.collider.Axis
import org.joml.Vector3f
import kotlin.math.hypot

/**
 * ConeShape implements a cone shape primitive, centered around the origin and
 * aligned with the upAxis.
 *
 * @author jezek2
 */
open class ConeShape(val radius: Float, val height: Float, val upAxis: Axis) : ConvexInternalShape() {

    private val sinAngle = 1f / hypot(1f, height / radius)

    val upAxisId get() = upAxis.id
    val secondary get() = upAxis.secondary
    val tertiary get() = upAxis.tertiary

    override fun getVolume(): Float {
        return radius * radius * height / 3f
    }

    private fun coneLocalSupport(v: Vector3f, out: Vector3f): Vector3f {
        val halfHeight = height * 0.5f
        if (v[upAxisId] > v.length() * sinAngle) {
            out[secondary] = 0f
            out[upAxisId] = halfHeight
            out[tertiary] = 0f
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
                out[secondary] = 0f
                out[upAxisId] = -halfHeight
                out[tertiary] = 0f
            }
        }
        return out
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f {
        return coneLocalSupport(dir, out)
    }

    override fun localGetSupportingVertex(dir: Vector3f, out: Vector3f): Vector3f {
        val supVertex = coneLocalSupport(dir, out)
        if (margin != 0f) {
            val vecNorm = Stack.newVec3f(dir)
            if (vecNorm.lengthSquared() < BulletGlobals.FLT_EPSILON_SQ) {
                vecNorm.set(-1.0, -1.0, -1.0)
            }
            vecNorm.normalize()
            supVertex.fma(margin, vecNorm)
            Stack.subVec3f(1)
        }
        return supVertex
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CONE

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        // todo surely, there's a better formula than just the box inertia...
        // also, should the margin be part of this? -> yes, better that way
        val margin = margin
        boxInertia(radius + margin, radius + margin, height * 0.5f + margin, mass, inertia)
        return inertia
    }
}
