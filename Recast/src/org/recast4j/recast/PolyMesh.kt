/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
Recast4J Copyright (c) 2015 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.recast

import org.joml.Vector3f

/**
 * Represents a polygon mesh suitable for use in building a navigation mesh.
 */
class PolyMesh {
    /**
     * The mesh vertices. [Form: (x, y, z) coordinates * #nvertices]
     */
    lateinit var vertices: IntArray

    /**
     * Polygon and neighbor data. [Length: #maxpolys * 2 * #nvp]
     */
    lateinit var polygons: IntArray

    /**
     * The region id assigned to each polygon. [Length: #maxpolys]
     */
    lateinit var regionIds: IntArray

    /**
     * The area id assigned to each polygon. [Length: #maxpolys]
     */
    lateinit var areaIds: IntArray

    var numVertices = 0
    var numPolygons = 0
    var maxVerticesPerPolygon = 0
    var numAllocatedPolygons = 0

    /**
     * The user defined flags for each polygon. [Length: #maxpolys]
     */
    lateinit var flags: IntArray

    /**
     * The bounds.
     */
    val bmin = Vector3f()
    val bmax = Vector3f()
    var cellSize = 0f
    var cellHeight = 0f

    /**
     * The AABB border size used to generate the source data from which the mesh was derived.
     */
    var borderSize = 0

    /**
     * The max error of the polygon edges in the mesh.
     */
    var maxEdgeError = 0f
}