package com.bulletphysics

import com.bulletphysics.linearmath.CProfileManager
import com.bulletphysics.linearmath.Clock

/**
 * Bullet statistics and profile support.
 *
 * @author jezek2
 */
object BulletStats {

    @JvmField
    var totalContactPoints: Int = 0

    // GjkPairDetector
    // temp globals, to improve GJK/EPA/penetration calculations
    @JvmField
    var numDeepPenetrationChecks: Int = 0
    @JvmField
    var numGjkChecks: Int = 0
    @JvmField
    var numSplitImpulseRecoveries: Int = 0

    @JvmField
    var overlappingPairs: Int = 0
    @JvmField
    var removedPairs: Int = 0
    @JvmField
    var addedPairs: Int = 0
    @JvmField
    var findPairCalls: Int = 0

    @JvmField
    val profileClock: Clock = Clock()

    // DiscreteDynamicsWorld:
    @JvmField
    var numClampedCcdMotions: Int = 0

    // JAVA NOTE: added for statistics in applet demo
    @JvmField
    var stepSimulationTime: Long = 0

    /**///////////////////////////////////////////////////////////////////////// */
    var isProfileEnabled: Boolean = false

    @JvmStatic
    fun profileGetTicks(): Long {
        return profileClock.timeNanos
    }

    @JvmStatic
    fun profileGetTickRate(): Double {
        return 1e6
    }

    /**
     * Pushes profile node. Use try/finally block to call [.popProfile] method.
     *
     * @param name must be [interned][String.intern] String (not needed for String literals)
     */
    @JvmStatic
    fun pushProfile(name: String?) {
        if (isProfileEnabled) {
            CProfileManager.startProfile(name)
        }
    }

    /**
     * Pops profile node.
     */
    @JvmStatic
    fun popProfile() {
        if (isProfileEnabled) {
            CProfileManager.stopProfile()
        }
    }
}
