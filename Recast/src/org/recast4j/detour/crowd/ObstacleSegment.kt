package org.recast4j.detour.crowd

import org.joml.Vector3f

class ObstacleSegment {
    /**
     * End points of the obstacle segment
     */
    val p = Vector3f()

    /**
     * End points of the obstacle segment
     */
    val q = Vector3f()
    var touch = false
}