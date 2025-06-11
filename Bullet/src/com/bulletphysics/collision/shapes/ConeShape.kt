package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.VectorUtil.getCoord
import com.bulletphysics.linearmath.VectorUtil.setCoord
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setScaleAdd
import kotlin.math.sqrt

/**
 * ConeShape implements a cone shape primitive, centered around the origin and
 * aligned with the upAxis.
 *
 * @author jezek2
 */
open class ConeShape(val radius: Double, val height: Double, upAxis: Int) : ConvexInternalShape() {

    private val sinAngle: Double = (radius / sqrt(this.radius * this.radius + this.height * this.height))
    private val coneIndices = IntArray(3)

    private fun coneLocalSupport(v: Vector3d, out: Vector3d): Vector3d {
        val halfHeight = height * 0.5
        if (getCoord(v, coneIndices[1]) > v.length() * sinAngle) {
            setCoord(out, coneIndices[0], 0.0)
            setCoord(out, coneIndices[1], halfHeight)
            setCoord(out, coneIndices[2], 0.0)
        } else {
            val v0 = getCoord(v, coneIndices[0])
            val v2 = getCoord(v, coneIndices[2])
            val s = sqrt(v0 * v0 + v2 * v2)
            if (s > BulletGlobals.FLT_EPSILON) {
                val d = radius / s
                setCoord(out, coneIndices[0], getCoord(v, coneIndices[0]) * d)
                setCoord(out, coneIndices[1], -halfHeight)
                setCoord(out, coneIndices[2], getCoord(v, coneIndices[2]) * d)
            } else {
                setCoord(out, coneIndices[0], 0.0)
                setCoord(out, coneIndices[1], -halfHeight)
                setCoord(out, coneIndices[2], 0.0)
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

    override fun calculateLocalInertia(mass: Double, inertia: Vector3d) {
        val identity = Stack.newTrans()
        identity.setIdentity()
        val aabbMin = Stack.newVec()
        val aabbMax = Stack.newVec()
        getAabb(identity, aabbMin, aabbMax)

        val halfExtents = Stack.newVec()
        aabbMax.sub(aabbMin, halfExtents).mul(0.5)

        val margin = margin
        val lx = 2.0 * (halfExtents.x + margin)
        val ly = 2.0 * (halfExtents.y + margin)
        val lz = 2.0 * (halfExtents.z + margin)
        val x2 = lx * lx
        val y2 = ly * ly
        val z2 = lz * lz
        val scaledMass = mass * 0.08333333f

        inertia.set(y2 + z2, x2 + z2, x2 + y2)
        inertia.mul(scaledMass)

        Stack.subVec(3)
        Stack.subTrans(1)
    }

    var upAxis: Int
        get() = coneIndices[1]
        set(upIndex) {
            when (upIndex) {
                0 -> {
                    coneIndices[0] = 1
                    coneIndices[1] = 0
                    coneIndices[2] = 2
                }
                1 -> {
                    coneIndices[0] = 0
                    coneIndices[1] = 1
                    coneIndices[2] = 2
                }
                else -> {
                    coneIndices[0] = 0
                    coneIndices[1] = 2
                    coneIndices[2] = 1
                }
            }
        }

    init {
        this.upAxis = upAxis
    }
}
