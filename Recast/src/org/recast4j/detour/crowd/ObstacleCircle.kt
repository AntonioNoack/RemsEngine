package org.recast4j.detour.crowd

import org.joml.Vector3f

class ObstacleCircle {
    /**
     * Position of the obstacle
     */
    val p = Vector3f()

    /**
     * Velocity of the obstacle
     */
    val vel = Vector3f()

    /**
     * Velocity of the obstacle
     */
    val dvel = Vector3f()

    /**
     * Radius of the obstacle
     */
    var rad = 0f

    /**
     * Use for side selection during sampling.
     */
    val dp = Vector3f()

    /**
     * Use for side selection during sampling.
     */
    val np = Vector3f()
}