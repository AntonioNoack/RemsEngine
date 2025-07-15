package com.bulletphysics

import com.bulletphysics.util.ArrayPool
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack

/**
 * Bullet global settings and constants.
 *
 * @author jezek2
 */
@Suppress("unused")
object BulletGlobals {
    var DEBUG: Boolean = false

    const val CONVEX_DISTANCE_MARGIN: Double = 0.04

    // we may have to change that to the correct double value
    const val FLT_EPSILON: Double = 2.220446049250313E-16
    const val FLT_EPSILON_SQ: Double = FLT_EPSILON * FLT_EPSILON
    const val SIMD_EPSILON: Double = FLT_EPSILON

    const val SIMD_TAU: Double = Math.PI * 2.0
    const val SIMD_PI: Double = Math.PI
    const val SIMD_HALF_PI: Double = SIMD_TAU * 0.25
    const val SIMD_RADS_PER_DEG: Double = SIMD_TAU / 360.0
    const val SIMD_DEGS_PER_RAD: Double = 360.0 / SIMD_TAU
    const val SIMD_INFINITY: Double = Double.MAX_VALUE

    /** ///////////////////////////////////////////////////////////////////////// */
    private val INSTANCES = ThreadLocal.withInitial { Globals() }
    private val INSTANCE: Globals get() = INSTANCES.get()

    var contactAddedCallback: ContactAddedCallback?
        get() = INSTANCE.contactAddedCallback
        set(callback) {
            INSTANCE.contactAddedCallback = callback
        }

    var contactDestroyedCallback: ContactDestroyedCallback?
        get() = INSTANCE.contactDestroyedCallback
        set(callback) {
            INSTANCE.contactDestroyedCallback = callback
        }

    var contactProcessedCallback: ContactProcessedCallback?
        get() = INSTANCE.contactProcessedCallback
        set(callback) {
            INSTANCE.contactProcessedCallback = callback
        }

    var contactBreakingThreshold: Double
        /** ///////////////////////////////////////////////////////////////////////// */
        get() = INSTANCE.contactBreakingThreshold
        set(threshold) {
            INSTANCE.contactBreakingThreshold = threshold
        }

    var deactivationTime: Double
        get() = INSTANCE.deactivationTime
        set(time) {
            INSTANCE.deactivationTime = time
        }

    var isDeactivationDisabled: Boolean
        get() = INSTANCE.disableDeactivation
        set(disable) {
            INSTANCE.disableDeactivation = disable
        }

    /**
     * Cleans all current thread specific settings and caches.
     */
    fun cleanCurrentThread() {
        INSTANCES.remove()
        Stack.libraryCleanCurrentThread()
        ObjectPool.cleanCurrentThread()
        ArrayPool.cleanCurrentThread()
    }

    private class Globals {
        var contactDestroyedCallback: ContactDestroyedCallback? = null
        var contactAddedCallback: ContactAddedCallback? = null
        var contactProcessedCallback: ContactProcessedCallback? = null

        var contactBreakingThreshold = 0.02

        // RigidBody
        var deactivationTime = 0.1
        var disableDeactivation = false
    }
}
