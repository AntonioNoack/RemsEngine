package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.AabbUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.mul
import com.bulletphysics.linearmath.VectorUtil.setCoord
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setScale
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
open class CapsuleShape(radius: Double, height: Double, var upAxis: Int = 0) : ConvexInternalShape() {

    init {
        implicitShapeDimensions.set(radius, radius, radius)
        setCoord(implicitShapeDimensions, upAxis, 0.5 * height)
    }

    override fun localGetSupportingVertexWithoutMargin(dir: Vector3d, out: Vector3d): Vector3d {
        val supVec = out
        supVec.set(0.0, 0.0, 0.0)

        var maxDot = -1e308

        val vec = Stack.newVec(dir)
        val lenSqr = vec.lengthSquared()
        if (lenSqr < 0.0001f) {
            vec.set(1.0, 0.0, 0.0)
        } else {
            val rlen = 1.0 / sqrt(lenSqr)
            vec.mul(rlen)
        }

        val vtx = Stack.newVec()
        var newDot: Double

        val radius = this.radius

        val tmp1 = Stack.newVec()
        val tmp2 = Stack.newVec()
        val pos = Stack.newVec()

        run {
            pos.set(0.0, 0.0, 0.0)
            setCoord(pos, this.upAxis, this.halfHeight)

            mul(tmp1, vec, localScaling)
            tmp1.mul(radius)
            tmp2.setScale(margin, vec)
            vtx.setAdd(pos, tmp1)
            vtx.sub(tmp2)
            newDot = vec.dot(vtx)
            if (newDot > maxDot) {
                maxDot = newDot
                supVec.set(vtx)
            }
        }

        run {
            pos.set(0.0, 0.0, 0.0)
            setCoord(pos, this.upAxis, -this.halfHeight)

            mul(tmp1, vec, localScaling)
            tmp1.mul(radius)
            tmp2.setScale(margin, vec)
            vtx.setAdd(pos, tmp1)
            vtx.sub(tmp2)
            newDot = vec.dot(vtx)
            if (newDot > maxDot) {
                maxDot = newDot
                supVec.set(vtx)
            }
        }

        return out
    }

    override fun batchedUnitVectorGetSupportingVertexWithoutMargin(
        dirs: Array<Vector3d>,
        outs: Array<Vector3d>,
        numVectors: Int
    ) {
        // TODO: implement
        throw UnsupportedOperationException("Not supported yet.")
    }

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d) {
        // as an approximation, take the inertia of the box that bounds the spheres

        val ident = Stack.newTrans()
        ident.setIdentity()

        val radius = this.radius

        val halfExtents = Stack.newVec()
        halfExtents.set(radius, radius, radius)
        setCoord(halfExtents, this.upAxis, radius + this.halfHeight)

        val margin = BulletGlobals.CONVEX_DISTANCE_MARGIN

        val lx = 2.0 * (halfExtents.x + margin)
        val ly = 2.0 * (halfExtents.y + margin)
        val lz = 2.0 * (halfExtents.z + margin)
        val x2 = lx * lx
        val y2 = ly * ly
        val z2 = lz * lz
        val scaledMass = mass * INV_12

        inertia.x = scaledMass * (y2 + z2)
        inertia.y = scaledMass * (x2 + z2)
        inertia.z = scaledMass * (x2 + y2)

        Stack.subVec(1)
        Stack.subTrans(1)
    }

    override val shapeType: BroadphaseNativeType
        get() = BroadphaseNativeType.CAPSULE_SHAPE_PROXYTYPE

    override fun getAabb(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val radius = radius
        val halfExtents = Stack.newVec()
        halfExtents.set(radius)
        setCoord(halfExtents, upAxis, radius + halfHeight)

        AabbUtil.transformAabb(halfExtents, margin, t, aabbMin, aabbMax)
        Stack.subVec(1)
    }

    val radius: Double
        get() {
            val radiusAxis = (upAxis + 2) % 3
            return getCoord(implicitShapeDimensions, radiusAxis)
        }

    val halfHeight: Double
        get() = getCoord(implicitShapeDimensions, upAxis)

    companion object {
        private const val INV_12 = 1.0 / 12.0
    }
}
