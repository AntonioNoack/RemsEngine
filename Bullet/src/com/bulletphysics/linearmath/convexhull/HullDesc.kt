package com.bulletphysics.linearmath.convexhull

import org.joml.Vector3d

/**
 * Describes point cloud data and other input for conversion to polygonal representation.
 *
 * @author jezek2
 */
class HullDesc {
    /**
     * Flags to use when generating the convex hull, see [HullFlags].
     */
    var flags: Int = HullFlags.DEFAULT

    /**
     * Number of vertices in the input point cloud.
     */
    @JvmField
    var vcount: Int = 0

    /**
     * Array of vertices.
     */
    @JvmField
    var vertices: List<Vector3d>? = null

    /**
     * Epsilon value for removing duplicates. This is a normalized value, if normalized bit is on.
     */
    var normalEpsilon: Double = 0.001

    /**
     * Maximum number of vertices to be considered for the hull.
     */
    @JvmField
    var maxVertices: Int = 4096

    constructor()

    constructor(flag: Int, vcount: Int, vertices: List<Vector3d>) {
        this.flags = flag
        this.vcount = vcount
        this.vertices = vertices
    }

    fun hasHullFlag(flag: Int): Boolean {
        return (flags and flag) != 0
    }

    fun setHullFlag(flag: Int) {
        flags = flags or flag
    }

    fun clearHullFlag(flag: Int) {
        flags = flags and flag.inv()
    }
}
