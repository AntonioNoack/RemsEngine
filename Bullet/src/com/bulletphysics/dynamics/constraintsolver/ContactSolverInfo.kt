package com.bulletphysics.dynamics.constraintsolver

/**
 * Current state of contact solver.
 *
 * @author jezek2
 */
class ContactSolverInfo {
    var tau: Double = 0.6
    var damping: Double = 1.0
    var friction: Double = 0.3
    var timeStep: Double = 0.0
    var restitution: Double = 0.0
    var numIterations: Int = 10
    var maxErrorReduction: Double = 20.0

    /**
     * Successive over-relaxation
     */
    var sor: Double = 1.3

    /**
     * used as Baumgarte factor
     */
    var erp: Double = 0.2

    /**
     * used in Split Impulse
     */
    var erp2: Double = 0.1
    var splitImpulse: Boolean = false
    var splitImpulsePenetrationThreshold: Double = -0.02
    var linearSlop: Double = 0.0
    var warmstartingFactor: Double = 0.85

    var solverMode: Int =
        SolverMode.SOLVER_RANDOMIZE_ORDER or SolverMode.SOLVER_CACHE_FRIENDLY or SolverMode.SOLVER_USE_WARMSTARTING

    constructor()

    constructor(g: ContactSolverInfo) {
        tau = g.tau
        damping = g.damping
        friction = g.friction
        timeStep = g.timeStep
        restitution = g.restitution
        numIterations = g.numIterations
        maxErrorReduction = g.maxErrorReduction
        sor = g.sor
        erp = g.erp
        erp2 = g.erp2
        splitImpulse = g.splitImpulse
        splitImpulsePenetrationThreshold = g.splitImpulsePenetrationThreshold
        linearSlop = g.linearSlop
        warmstartingFactor = g.warmstartingFactor
        solverMode = g.solverMode
    }
}
