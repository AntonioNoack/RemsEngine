package com.bulletphysics.linearmath.convexhull

import org.joml.Vector3d

/**
 * Describes point cloud data and other input for conversion to polygonal representation.
 *
 * @author jezek2
 */
class HullDesc(
    val vertices: List<Vector3d>,
    var maxVertices: Int = 4096,
    var normalEpsilon: Double = 0.001
)