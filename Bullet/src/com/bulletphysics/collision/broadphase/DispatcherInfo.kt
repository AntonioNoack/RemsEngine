package com.bulletphysics.collision.broadphase

import com.bulletphysics.linearmath.IDebugDraw

/**
 * Current state of [Dispatcher].
 *
 * @author jezek2
 */
class DispatcherInfo {
    var timeOfImpact = 1f
    var timeStep: Float = 1f / 60f
    var allowedCcdPenetration = 0.04f

    var discreteDispatch = true
    var debugDraw: IDebugDraw? = null
}
