package com.bulletphysics.dynamics.vehicle

/**
 * Vehicle tuning parameters.
 *
 * @author jezek2
 */
class VehicleTuning {
    @JvmField
    var suspensionStiffness = 5.88f

    @JvmField
    var suspensionCompression = 0.83f

    @JvmField
    var suspensionDamping = 0.88f

    @JvmField
    var maxSuspensionTravel = 5f

    @JvmField
    var frictionSlip = 10.5f
}
