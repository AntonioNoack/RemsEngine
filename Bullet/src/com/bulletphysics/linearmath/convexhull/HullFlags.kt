package com.bulletphysics.linearmath.convexhull

/**
 * Flags that affects convex hull generation, used in [HullDesc.flags].
 *
 * @author jezek2
 */
object HullFlags {
    var TRIANGLES: Int = 1 // report results as triangles, not polygons.
    var REVERSE_ORDER: Int = 2 // reverse order of the triangle indices.
    var DEFAULT: Int = TRIANGLES
}
