package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import kotlin.math.max
import kotlin.math.min

/**
 * CollisionShape class provides an interface for collision shapes that can be
 * shared among [com.bulletphysics.collision.dispatch.CollisionObject]s.
 *
 * @author jezek2
 */
abstract class CollisionShape {

    /**getAabb returns the axis aligned bounding box in the coordinate frame of the given transform t. */
    abstract fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d)

    open fun getVolume(): Double {
        // use bounds as volume guess
        val aabbMin = Stack.newVec()
        val aabbMax = Stack.newVec()

        val identity = Stack.newTrans()
        identity.setIdentity()
        getBounds(identity, aabbMin, aabbMax)

        val volume = max(aabbMax.x - aabbMin.x, 0.0) *
                max(aabbMax.y - aabbMin.y, 0.0) *
                max(aabbMax.z - aabbMin.z, 0.0)

        Stack.subTrans(1)
        Stack.subVec(2)
        return volume
    }

    fun getBoundingSphere(center: Vector3d): Double {
        val tmp = Stack.newVec()

        val tr = Stack.newTrans()
        tr.setIdentity()
        val aabbMin = Stack.newVec()
        val aabbMax = Stack.newVec()

        getBounds(tr, aabbMin, aabbMax)

        aabbMax.sub(aabbMin, tmp)
        val dst = tmp.length() * 0.5 // halfExtents.length()

        aabbMin.add(aabbMax, tmp)
        tmp.mul(0.5, center)

        Stack.subVec(3)
        Stack.subTrans(1)

        return dst
    }

    /**
     * maximum radius needed for Conservative Advancement to handle time-of-impact with rotations
     * */
    open val angularMotionDisc: Double
        get() {
            val center = Stack.newVec()
            var dst = getBoundingSphere(center)
            dst += center.length()
            Stack.subVec(1)
            return dst
        }

    /**
     * calculates the enclosing aabb for the moving object over interval [0, timeStep]
     * result is conservative
     * */
    fun calculateTemporalAabb(
        curTrans: Transform,
        linVel: Vector3d,
        angVelLength: Double,
        timeStep: Double,
        dstAabbMin: Vector3d,
        dstAabbMax: Vector3d
    ) {
        // get static aabb
        getBounds(curTrans, dstAabbMin, dstAabbMax)

        val stepX = linVel.x * timeStep
        val stepY = linVel.y * timeStep
        val stepZ = linVel.z * timeStep

        // add conservative angular motion
        val angularMotion = angVelLength * angularMotionDisc * timeStep

        // only add if < 0 | > 0
        dstAabbMin.add(
            min(stepX, 0.0) - angularMotion,
            min(stepY, 0.0) - angularMotion,
            min(stepZ, 0.0) - angularMotion
        )
        dstAabbMax.add(
            max(stepX, 0.0) + angularMotion,
            max(stepY, 0.0) + angularMotion,
            max(stepZ, 0.0) + angularMotion
        )
    }

    abstract val shapeType: BroadphaseNativeType

    abstract fun setLocalScaling(scaling: Vector3d)

    abstract fun getLocalScaling(out: Vector3d): Vector3d

    abstract fun calculateLocalInertia(mass: Double, inertia: Vector3d): Vector3d

    open var margin: Double = BulletGlobals.CONVEX_DISTANCE_MARGIN

    /**
     * can be used as a multiplier for volume
     * */
    var density = 1.0
}
