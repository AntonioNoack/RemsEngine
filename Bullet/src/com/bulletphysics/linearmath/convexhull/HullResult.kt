/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Stan Melax Convex Hull Computation
 * Copyright (c) 2008 Stan Melax http://www.melax.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */
package com.bulletphysics.linearmath.convexhull

import com.bulletphysics.util.IntArrayList
import org.joml.Vector3d

/**
 * Contains resulting polygonal representation.
 *
 *
 *
 * Depending on the [.polygons] flag, array of indices consists of:<br></br>
 * **for triangles:** indices are array indexes into the vertex list<br></br>
 * **for polygons:** indices are in the form (number of points in face) (p1, p2, p3, ...)
 *
 * @author jezek2
 */
class HullResult {
    /** True if indices represents polygons, false indices are triangles.  */
    var polygons: Boolean = true

    /** Number of vertices in the output hull.  */
    var numOutputVertices: Int = 0

    /** Array of vertices.  */
	@JvmField
	val outputVertices = ArrayList<Vector3d>()

    /** Number of faces produced.  */
    var numFaces: Int = 0

    /** Total number of indices.  */
    var numIndices: Int = 0

    /** Array of indices.  */
    val indices: IntArrayList = IntArrayList()
}
