package com.bulletphysics.dynamics.constraintsolver

/**
 * Current state of contact solver.
 *
 * @author jezek2
 */
class ContactSolverInfo {
    var tau = 0.6f
    var damping = 1f
    var friction = 0.3f
    var timeStep: Float = 0f
    var restitution = 0f
    var numIterations = 10
    var maxErrorReduction = 20f

    var successiveOverRelaxation = 1.3f
    var baumgarteFactor = 0.2f

    /**
     * used in Split Impulse
     */
    var erp2 = 0.1f
    var splitImpulse: Boolean = false
    var splitImpulsePenetrationThreshold = -0.02f
    var linearSlop = 0.0f
    var warmstartingFactor = 0.85f

    var solverMode: Int =
        SolverMode.SOLVER_RANDOMIZE_ORDER or SolverMode.SOLVER_CACHE_FRIENDLY or SolverMode.SOLVER_USE_WARMSTARTING

    constructor()
}
