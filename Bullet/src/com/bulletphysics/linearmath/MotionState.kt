package com.bulletphysics.linearmath

/**
 * MotionState allows the dynamics world to synchronize the updated world transforms
 * with graphics. For optimizations, potentially only moving objects get synchronized
 * (using [setWorldTransform][.setWorldTransform] method).
 *
 * @author jezek2
 */
interface MotionState {
    /**
     * Returns world transform.
     */
    fun getWorldTransform(out: Transform): Transform

    /**
     * Sets world transform. This method is called by JBullet whenever an active
     * object represented by this MotionState is moved or rotated.
     */
    fun setWorldTransform(worldTrans: Transform)
}
