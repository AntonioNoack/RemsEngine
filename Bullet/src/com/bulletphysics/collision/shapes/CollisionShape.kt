package com.bulletphysics.collision.shapes

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub

/**
 * CollisionShape class provides an interface for collision shapes that can be
 * shared among [com.bulletphysics.collision.dispatch.CollisionObject]s.
 *
 * @author jezek2
 */
abstract class CollisionShape {

    // optional user data pointer
    var userPointer: Any? = null

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
        val dst = tmp.length() * 0.5

        tmp.setAdd(aabbMin, aabbMax)
        center.setScale(0.5, tmp)

        Stack.subVec(3)
        Stack.subTrans(1)

        return dst
    }

    val angularMotionDisc: Double
        /**getAngularMotionDisc returns the maximus radius needed for Conservative Advancement to handle time-of-impact with rotations. */
        get() {
            val center = Stack.newVec()
            var dst = getBoundingSphere(center)
            dst += center.length()
            Stack.subVec(1)
            return dst
        }

    /**calculateTemporalAabb calculates the enclosing aabb for the moving object over interval [0..timeStep)
     * result is conservative */
    fun calculateTemporalAabb(
        curTrans: Transform,
        linVel: Vector3d,
        angVel: Vector3d,
        timeStep: Double,
        temporalAabbMin: Vector3d,
        temporalAabbMax: Vector3d
    ) {
        //start with static aabb
        getAabb(curTrans, temporalAabbMin, temporalAabbMax)

        var temporalAabbMaxx = temporalAabbMax.x
        var temporalAabbMaxy = temporalAabbMax.y
        var temporalAabbMaxz = temporalAabbMax.z
        var temporalAabbMinx = temporalAabbMin.x
        var temporalAabbMiny = temporalAabbMin.y
        var temporalAabbMinz = temporalAabbMin.z

        // add linear motion
        val linMotion = Stack.newVec(linVel)
        linMotion.mul(timeStep)

        //todo: simd would have a vector max/min operation, instead of per-element access
        if (linMotion.x > 0.0) {
            temporalAabbMaxx += linMotion.x
        } else {
            temporalAabbMinx += linMotion.x
        }
        if (linMotion.y > 0.0) {
            temporalAabbMaxy += linMotion.y
        } else {
            temporalAabbMiny += linMotion.y
        }
        if (linMotion.z > 0.0) {
            temporalAabbMaxz += linMotion.z
        } else {
            temporalAabbMinz += linMotion.z
        }

        //add conservative angular motion
        val angularMotion = angVel.length() * this.angularMotionDisc * timeStep
        val angularMotion3d = Stack.newVec()
        angularMotion3d.set(angularMotion, angularMotion, angularMotion)
        temporalAabbMin.set(temporalAabbMinx, temporalAabbMiny, temporalAabbMinz)
        temporalAabbMax.set(temporalAabbMaxx, temporalAabbMaxy, temporalAabbMaxz)

        temporalAabbMin.sub(angularMotion3d)
        temporalAabbMax.add(angularMotion3d)
        Stack.subVec(1)
    }

    val isPolyhedral: Boolean
        get() = this.shapeType.isPolyhedral

    val isConvex: Boolean
        get() = this.shapeType.isConvex

    val isConcave: Boolean
        get() = this.shapeType.isConcave

    val isCompound: Boolean
        get() = this.shapeType.isCompound

    /**isInfinite is used to catch simulation error (aabb check) */
    val isInfinite: Boolean
        get() = this.shapeType.isInfinite

    abstract val shapeType: BroadphaseNativeType

    abstract fun setLocalScaling(scaling: Vector3d)

    abstract fun getLocalScaling(out: Vector3d): Vector3d

    abstract fun calculateLocalInertia(mass: Double, inertia: Vector3d)

    open var margin: Double = BulletGlobals.CONVEX_DISTANCE_MARGIN
}
