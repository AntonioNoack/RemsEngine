package org.recast4j.detour.crowd

import org.joml.Vector3f

class ObstacleSegment {
    val segmentStart = Vector3f()
    val segmentEnd = Vector3f()
    var isTouchedByAgent = false
}