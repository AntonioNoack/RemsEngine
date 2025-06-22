package com.bulletphysics.linearmath

/**
 * DefaultMotionState provides a common implementation to synchronize world transforms
 * with offsets.
 *
 * @author jezek2
 */
@Suppress("unused")
class DefaultMotionState : MotionState {

    /** Current interpolated world transform, used to draw object.  */
    val graphicsWorldTrans = Transform()

    /** Center of mass offset transform, used to adjust graphics world transform.  */
    val centerOfMassOffset = Transform()

    /** Initial world transform.  */
    val startWorldTrans = Transform()

    /**
     * Creates a new DefaultMotionState with all transforms set to identity.
     */
    constructor() {
        graphicsWorldTrans.setIdentity()
        centerOfMassOffset.setIdentity()
        startWorldTrans.setIdentity()
    }

    /**
     * Creates a new DefaultMotionState with initial world transform and center
     * of mass offset transform set to identity.
     */
    constructor(startTrans: Transform) {
        this.graphicsWorldTrans.set(startTrans)
        centerOfMassOffset.setIdentity()
        this.startWorldTrans.set(startTrans)
    }

    /**
     * Creates a new DefaultMotionState with initial world transform and center
     * of mass offset transform.
     */
    constructor(startTrans: Transform, centerOfMassOffset: Transform) {
        this.graphicsWorldTrans.set(startTrans)
        this.centerOfMassOffset.set(centerOfMassOffset)
        this.startWorldTrans.set(startTrans)
    }

    override fun getWorldTransform(out: Transform): Transform {
        out.inverse(centerOfMassOffset)
        out.mul(graphicsWorldTrans)
        return out
    }

    override fun setWorldTransform(worldTrans: Transform) {
        graphicsWorldTrans.set(worldTrans)
        graphicsWorldTrans.mul(centerOfMassOffset)
    }
}
