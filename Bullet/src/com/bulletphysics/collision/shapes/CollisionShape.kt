package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub
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
    abstract fun getAabb(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d)

    fun getBoundingSphere(center: Vector3d): Double {
        val tmp = Stack.newVec()

        val tr = Stack.newTrans()
        tr.setIdentity()
        val aabbMin = Stack.newVec()
        val aabbMax = Stack.newVec()

        getAabb(tr, aabbMin, aabbMax)

        tmp.setSub(aabbMax, aabbMin)
        val dst = tmp.length() * 0.5 // halfExtents.length()

        tmp.setAdd(aabbMin, aabbMax)
        center.setScale(0.5, tmp)

        Stack.subVec(3)
        Stack.subTrans(1)

        return dst
    }

    /**getAngularMotionDisc returns the maximus radius needed for Conservative Advancement to handle time-of-impact with rotations. */
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
        getAabb(curTrans, dstAabbMin, dstAabbMax)

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

    val isCompound: Boolean
        get() = shapeType.isCompound

    abstract val shapeType: BroadphaseNativeType

    abstract fun setLocalScaling(scaling: Vector3d)

    abstract fun getLocalScaling(out: Vector3d): Vector3d

    abstract fun calculateLocalInertia(mass: Double, inertia: Vector3d)

    open var margin: Double = BulletGlobals.CONVEX_DISTANCE_MARGIN
}
