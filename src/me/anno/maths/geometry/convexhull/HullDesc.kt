package me.anno.maths.geometry.convexhull

import org.joml.Vector3d

/**
 * Describes point cloud data and other input for conversion to polygonal representation.
 *
 * @author jezek2
 */
class HullDesc(
    var vertices: List<Vector3d>,
    var maxNumVertices: Int = 4096,
    var normalEpsilon: Double = 0.001
)