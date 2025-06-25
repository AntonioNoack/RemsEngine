package org.recast4j.detour.crowd

import org.joml.Vector3f

class ObstacleCircle {
    /**
     * Position of the obstacle
     */
    val position = Vector3f()

    /**
     * Velocity of the obstacle
     */
    val actualVelocity = Vector3f()

    /**
     * Velocity of the obstacle
     */
    val desiredVelocity = Vector3f()

    /**
     * Radius of the obstacle
     */
    var radius = 0f

    /**
     * Use for side selection during sampling.
     */
    val dp = Vector3f()

    /**
     * Use for side selection during sampling.
     */
    val np = Vector3f()
}