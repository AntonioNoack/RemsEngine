package com.bulletphysics.collision.shapes

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
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

    val radius get() = margin

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {
        out.set(0.0, 0.0, 0.0)
        return out
    }

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3d>, outs: Array<Vector3d>, numVectors: Int
    ) {
        for (i in 0 until numVectors) {
            outs[i].set(0.0, 0.0, 0.0)
        }
    }

    override fun getAabb(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val center = t.origin
        val margin = margin
        center.sub(margin, aabbMin)
        center.add(margin, aabbMax)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.SPHERE_SHAPE_PROXYTYPE

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d) {
        val radius = margin
        val elem = 0.4 * mass * radius * radius
        inertia.set(elem, elem, elem)
    }
}
