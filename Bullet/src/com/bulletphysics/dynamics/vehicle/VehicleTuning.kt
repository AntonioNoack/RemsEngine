package com.bulletphysics.dynamics.vehicle

/**
 * Vehicle tuning parameters.
 *
 * @author jezek2
 */
class VehicleTuning {
    @JvmField
    var suspensionStiffness: Double = 5.88

    @JvmField
    var suspensionCompression: Double = 0.83

    @JvmField
    var suspensionDamping: Double = 0.88

    @JvmField
    var maxSuspensionTravelCm: Double = 500.0

    @JvmField
    var frictionSlip: Double = 10.5
}
