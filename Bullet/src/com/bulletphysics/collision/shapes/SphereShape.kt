package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import org.joml.Vector3d

/**
 * SphereShape implements an implicit sphere, centered around a local origin with radius.
 *
 * @author jezek2
 */
class SphereShape(radius: Double) : ConvexInternalShape() {

    init {
        margin = radius
    }

    var radius: Double
        get() = margin
        set(value) {
            margin = value
        }

    override fun getVolume(): Double {
        return radius * radius * radius * 4.0 / 3.0 * Math.PI
    }

    // center.length + halfExtents
    override val angularMotionDisc: Double get() = margin

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {
        return out.set(0.0)
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val center = t.origin
        val margin = margin
        center.sub(margin, aabbMin)
        center.add(margin, aabbMax)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.SPHERE

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d {
        val radius = margin
        return inertia.set(0.4 * mass * radius * radius)
    }
}
