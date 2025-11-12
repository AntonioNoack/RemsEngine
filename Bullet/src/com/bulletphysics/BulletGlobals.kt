package com.bulletphysics

import com.bulletphysics.util.ArrayPool
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import kotlin.math.nextUp

/**
 * Bullet global settings and constants.
 *
 * @author jezek2
 */
@Suppress("unused")
object BulletGlobals {
    var DEBUG: Boolean = false

    const val CONVEX_DISTANCE_MARGIN = 0.04f

    // we may have to change that to the correct double value
    const val FLT_EPSILON = 1e-7f
    const val FLT_EPSILON_SQ = FLT_EPSILON * FLT_EPSILON
    const val SIMD_EPSILON = FLT_EPSILON

    const val SIMD_TAU = TAUf
    const val SIMD_PI = PIf
    const val SIMD_HALF_PI = SIMD_TAU * 0.25f
    const val SIMD_RADS_PER_DEG = SIMD_TAU / 360f
    const val SIMD_DEGS_PER_RAD = 360f / SIMD_TAU
    const val SIMD_INFINITY = Float.MAX_VALUE

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

    var contactBreakingThreshold: Float
        /** ///////////////////////////////////////////////////////////////////////// */
        get() = INSTANCE.contactBreakingThreshold
        set(threshold) {
            INSTANCE.contactBreakingThreshold = threshold
        }

    var deactivationTime: Float
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

        var contactBreakingThreshold = 0.02f

        // RigidBody
        var deactivationTime = 0.1f
        var disableDeactivation = false
    }
}
