package com.bulletphysics.collision.broadphase

import com.bulletphysics.linearmath.IDebugDraw

/**
 * Current state of [Dispatcher].
 *
 * @author jezek2
 */
class DispatcherInfo {
    @JvmField
    var timeStep: Double = 0.0

    @JvmField
    var stepCount: Int = 0
    var dispatchFunc = DispatchFunc.DISPATCH_DISCRETE
    var timeOfImpact: Double = 1.0

    @JvmField
    var debugDraw: IDebugDraw? = null
    var allowedCcdPenetration: Double = 0.04
}
