package com.bulletphysics.dynamics.vehicle

/**
 * Vehicle tuning parameters.
 *
 * @author jezek2
 */
class VehicleTuning {
    @JvmField
    var suspensionStiffness = 5.88

    @JvmField
    var suspensionCompression = 0.83

    @JvmField
    var suspensionDamping = 0.88

    @JvmField
    var maxSuspensionTravel = 5.0

    @JvmField
    var frictionSlip = 10.5
}
