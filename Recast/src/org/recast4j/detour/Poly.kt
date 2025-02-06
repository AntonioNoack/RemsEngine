/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.detour

/** Defines a polygon within a MeshTile object.  */
class Poly(val index: Int, maxVerticesPerPoly: Int) {
    /** The indices of the polygon's vertices. The actual vertices are located in MeshTile::vertices.  */
    val vertices: IntArray

    /** Packed data representing neighbor polygons references and flags for each edge.  */
    val neighborData: IntArray

    /** The user defined polygon flags.  */
    var flags = 0

    /** The number of vertices in the polygon.  */
    var vertCount = 0

    /**
     * The bit packed area id and polygon type.
     *
     * @note Use the structure's set and get methods to access this value.
     */
    var areaAndType = 0

    init {
        vertices = IntArray(maxVerticesPerPoly)
        neighborData = IntArray(maxVerticesPerPoly)
    }
    /** Gets the user defined area id.  */
    /** Sets the user defined area id. [Limit: &lt; [org.recast4j.detour.NavMesh.DT_MAX_AREAS]]  */
    var area: Int
        get() = areaAndType and 0x3f
        set(a) {
            areaAndType = areaAndType and 0xc0 or (a and 0x3f)
        }
    /** Gets the polygon type. (See: #dtPolyTypes)  */
    /** Sets the polygon type. (See: #dtPolyTypes.)  */
    var type: Int
        get() = areaAndType shr 6
        set(t) {
            areaAndType = areaAndType and 0x3f or (t shl 6)
        }

    companion object {
        /** The polygon is a standard convex polygon that is part of the surface of the mesh.  */
        const val DT_POLYTYPE_GROUND = 0

        /** The polygon is an off-mesh connection consisting of two vertices.  */
        const val DT_POLYTYPE_OFFMESH_CONNECTION = 1
    }
}