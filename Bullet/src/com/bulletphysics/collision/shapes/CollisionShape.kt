package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f
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

    open fun getVolume(): Float {
        // use bounds as volume guess
        val aabbMin = Stack.newVec3d()
        val aabbMax = Stack.newVec3d()

        val identity = Stack.newTrans()
        identity.setIdentity()
        getBounds(identity, aabbMin, aabbMax)

        val volume = max(aabbMax.x - aabbMin.x, 0.0) *
                max(aabbMax.y - aabbMin.y, 0.0) *
                max(aabbMax.z - aabbMin.z, 0.0)

        Stack.subTrans(1)
        Stack.subVec3d(2)
        return volume.toFloat()
    }

    fun getBoundingSphere(center: Vector3d?): Float {

        val tr = Stack.newTrans()
        tr.setIdentity()

        val aabbMin = Stack.newVec3d()
        val aabbMax = Stack.newVec3d()

        getBounds(tr, aabbMin, aabbMax)

        val dst = aabbMin.distance(aabbMax).toFloat() * 0.5f
        if (center != null) {
            aabbMin.add(aabbMax, center).mul(0.5)
        }

        Stack.subVec3d(2)
        Stack.subTrans(1)

        return dst
    }

    /**
     * maximum radius needed for Conservative Advancement to handle time-of-impact with rotations
     * */
    open val angularMotionDisc: Float
        get() {
            val center = Stack.newVec3d()
            var dst = getBoundingSphere(center)
            dst += center.length().toFloat()
            Stack.subVec3d(1)
            return dst
        }

    /**
     * calculates the enclosing aabb for the moving object over interval [0, timeStep]
     * result is conservative
     * */
    fun calculateTemporalAabb(
        curTrans: Transform,
        linVel: Vector3f,
        angVelLength: Float,
        timeStep: Float,
        dstAabbMin: Vector3d,
        dstAabbMax: Vector3d
    ) {
        // get static aabb
        getBounds(curTrans, dstAabbMin, dstAabbMax)

        val stepX = linVel.x * timeStep
        val stepY = linVel.y * timeStep
        val stepZ = linVel.z * timeStep

        // add conservative angular motion
        val angularMotion = (angVelLength * angularMotionDisc * timeStep).toDouble()

        // only add if < 0 | > 0
        dstAabbMin.add(
            min(stepX, 0f),
            min(stepY, 0f),
            min(stepZ, 0f)
        ).sub(angularMotion)
        dstAabbMax.add(
            max(stepX, 0f),
            max(stepY, 0f),
            max(stepZ, 0f)
        ).add(angularMotion)
    }

    abstract val shapeType: BroadphaseNativeType

    abstract fun calculateLocalInertia(mass: Float, inertia: Vector3f): Vector3f

    open var localScaling: Vector3f = Vector3f(1f)
    open var margin: Float = BulletGlobals.CONVEX_DISTANCE_MARGIN

    /**
     * can be used as a multiplier for volume
     * */
    var density = 1f
}
