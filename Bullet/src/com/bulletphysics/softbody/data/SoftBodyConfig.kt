package com.bulletphysics.softbody.data

import me.anno.ecs.annotations.Range

class SoftBodyConfig {
    /**
     * Aerodynamic model (default: V_Point)
     * */
    var aeroModel = 0

    /**
     * Velocities correction factor (Baumgarte)
     * */
    var baumgarte = 0f

    @Range(0.0, 1.0)
    var damping = 0f

    @Range(0.0, Double.POSITIVE_INFINITY)
    var drag = 0f

    @Range(0.0, Double.POSITIVE_INFINITY)
    var lift = 0f

    /**
     * Pressure coefficient [-inf,+inf]
     * */
    var pressure = 0f

    /**
     * Volume conversation coefficient
     * */
    @Range(0.0, Double.POSITIVE_INFINITY)
    var volume = 0f

    /**
     * Dynamic friction coefficient
     * */
    @Range(0.0, 1.0)
    var dynamicFriction = 0f

    /**
     * Pose matching coefficient
     * */
    @Range(0.0, 1.0)
    var poseMatch = 0f

    @Range(0.0, 1.0)
    var rigidContactHardness = 0f

    @Range(0.0, 1.0)
    var kineticContactHardness = 0f

    @Range(0.0, 1.0)
    var softContactHardness = 0f

    @Range(0.0, 1.0)
    var anchorHardness = 0f

    @Range(0.0, 1.0)
    var softRigidClusterHardness = 0f

    @Range(0.0, 1.0)
    var softKineticClusterHardness = 0f

    @Range(0.0, 1.0)
    var softSoftClusterHardness = 0f

    @Range(0.0, 1.0)
    var softRigidClusterImpulseSplit = 0f

    @Range(0.0, 1.0)
    var softKineticClusterImpulseSplit = 0f

    @Range(0.0, 1.0)
    var softSoftClusterImpulseSplit = 0f

    /**
     * Maximum volume ratio for pose
     * */
    var maxVolume = 1f

    var timeScale = 1f

    /**
     * num solver iterations
     * */
    var velocityIterations = 5
    var positionIterations = 5
    var driftIterations = 5
    var clusterIterations = 5

    var collisionFlags = 0

}