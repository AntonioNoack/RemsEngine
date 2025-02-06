package org.recast4j.detour.extras.jumplink

import org.joml.Vector3f

class GroundSegment {
    @JvmField
    val p = Vector3f()

    @JvmField
    val q = Vector3f()

    var samples: Array<GroundSample>? = null

    @JvmField
    var height = 0f
}