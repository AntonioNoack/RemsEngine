package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.mul
import cz.advel.stack.Stack
import me.anno.ecs.components.collider.Axis
import me.anno.maths.Maths.PIf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.sqrt

/**
 * CapsuleShape represents a capsule around the upAxis.
 *
 * The total height is height+2*radius, so the height is just the height between
 * the center of each "sphere" of the capsule caps.
 *
 * CapsuleShape is a convex hull of two spheres. The [MultiSphereShape] is
 * a more general collision shape that takes the convex hull of multiple sphere,
 * so it can also represent a capsule when just using two spheres.
 *
 * @author jezek2
 */
open class CapsuleShape(radius: Float, height: Float, val upAxis: Axis) : ConvexInternalShape() {

    init {
        implicitShapeDimensions.set(radius)
        implicitShapeDimensions[upAxis.id] = 0.5f * height
    }

    override fun getVolume(): Float {
        val radius = radius
        // common parts have been extracted
        val commonPart = radius * radius * PIf
        val sphereVolume = radius * 4f / 3f
        val cylinderVolume = halfHeight * 2f
        return (sphereVolume + cylinderVolume) * commonPart
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3f, out: Vector3f): Vector3f {

        out.set(0.0, 0.0, 0.0)

        var maxDot = -1e38f

        val vec = Stack.newVec3f(dir)
        val lenSqr = vec.lengthSquared()
        if (lenSqr < 0.0001f) {
            vec.set(1.0, 0.0, 0.0)
        } else {
            val rlen = 1f / sqrt(lenSqr)
            vec.mul(rlen)
        }

        val vtx = Stack.newVec3f()
        var newDot: Float

        val radius = radius

        val tmp1 = Stack.newVec3f()
        val tmp2 = Stack.newVec3f()
        val pos = Stack.newVec3f()

        run {
            pos.set(0.0, 0.0, 0.0)
            pos[upAxis.id] = halfHeight

            mul(tmp1, vec, localScaling)
            tmp1.mul(radius)
            vec.mul(margin, tmp2)
            pos.add(tmp1, vtx)
            vtx.sub(tmp2)
            newDot = vec.dot(vtx)
            if (newDot > maxDot) {
                maxDot = newDot
                out.set(vtx)
            }
        }

        run {
            pos.set(0.0, 0.0, 0.0)
            pos[upAxis.id] = -halfHeight

            mul(tmp1, vec, localScaling)
            tmp1.mul(radius)
            vec.mul(margin, tmp2)
            pos.add(tmp1, vtx)
            vtx.sub(tmp2)
            newDot = vec.dot(vtx)
            if (newDot > maxDot) {
                // maxDot = newDot
                out.set(vtx)
            }
        }

        Stack.subVec3f(5)
        return out
    }

    override fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f {
        // as an approximation, take the inertia of the box that bounds the spheres

        val radius = radius
        val halfExtents = Stack.newVec3f()
        halfExtents.set(radius, radius, radius)
        halfExtents[upAxis.id] = radius + halfHeight

        val margin = BulletGlobals.CONVEX_DISTANCE_MARGIN

        val lx = 2.0 * (halfExtents.x + margin)
        val ly = 2.0 * (halfExtents.y + margin)
        val lz = 2.0 * (halfExtents.z + margin)
        val x2 = lx * lx
        val y2 = ly * ly
        val z2 = lz * lz
        val scaledMass = mass * INV_12

        Stack.subVec3f(1)

        return inertia
            .set(y2 + z2, x2 + z2, x2 + y2)
            .mul(scaledMass)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CAPSULE

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val radius = radius
        val halfExtents = Stack.newVec3f()
        halfExtents.set(radius)
        halfExtents[upAxis.id] = radius + halfHeight

        AabbUtil.transformAabb(halfExtents, margin, t, aabbMin, aabbMax)
        Stack.subVec3f(1)
    }

    val radius: Float
        get() {
            val radiusAxis = (upAxis.id + 2) % 3
            return implicitShapeDimensions[radiusAxis]
        }

    val halfHeight: Float
        get() = implicitShapeDimensions[upAxis.id]

    companion object {
        private const val INV_12 = 1f / 12f
    }
}
