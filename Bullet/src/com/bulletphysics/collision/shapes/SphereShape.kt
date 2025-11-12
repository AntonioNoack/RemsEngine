package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.sq
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * SphereShape implements an implicit sphere, centered around a local origin with radius.
 *
 * @author jezek2
 */
class SphereShape(radius: Float) : ConvexInternalShape() {

    init {
        margin = radius
    }

    var radius: Float
        get() = margin
        set(value) {
            margin = value
        }

    override fun getVolume(): Float {
        return radius * radius * radius * 4f / 3f * PIf
    }

    // center.length + halfExtents
    override val angularMotionDisc: Float get() = margin

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f {
        return out.set(0f)
    }

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val center = t.origin
        val margin = margin.toDouble()
        center.sub(margin, aabbMin)
        center.add(margin, aabbMax)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.SPHERE

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        return inertia.set(0.4f * mass * sq(radius))
    }
}
