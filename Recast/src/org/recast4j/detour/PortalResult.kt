package org.recast4j.detour

import org.joml.Vector3f

class PortalResult(val left: Vector3f, val right: Vector3f, var fromType: Int, var toType: Int) {
    constructor() : this(Vector3f(), Vector3f(), 0, 0)
}