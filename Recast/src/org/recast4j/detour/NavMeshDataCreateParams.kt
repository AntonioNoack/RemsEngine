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

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.joml.Vector3f

/**
 * Represents the source data used to build a navigation mesh tile.
 */
class NavMeshDataCreateParams {

    companion object {
        val i0 = IntArray(0)
        val f0 = FloatArray(0)
    }

    /**
     * The polygon mesh vertices. [(x, y, z) * #vertCount] [Unit: vx]
     */
    @Nullable
    var vertices: IntArray? = null

    /**
     * The number vertices in the polygon mesh. [Limit: >= 3]
     */
    var vertCount = 0

    /**
     * The polygon data. [Size: #polyCount * 2 * #nvp]
     */
    @Nullable
    var polys: IntArray? = null

    /**
     * The user defined flags assigned to each polygon. [Size: #polyCount]
     */
    @NotNull
    var polyFlags: IntArray = i0

    /**
     * The user defined area ids assigned to each polygon. [Size: #polyCount]
     */
    @NotNull
    var polyAreas: IntArray = i0

    /**
     * Number of polygons in the mesh. [Limit: >= 1]
     */
    var polyCount = 0

    /**
     * Number maximum number of vertices per polygon. [Limit: >= 3]
     */
    var maxVerticesPerPolygon = 3

    /** @name Height Detail Attributes (Optional)
     *See #rcPolyMeshDetail for details related to these attributes.
     */

    /**
     * The height detail sub-mesh data. [Size: 4 * #polyCount]
     */
    @Nullable
    var detailMeshes: IntArray? = null

    /**
     * The detail mesh vertices. [Size: 3 * #detailVerticesCount]
     */
    @Nullable
    var detailVertices: FloatArray? = null

    /**
     * The number of vertices in the detail mesh.
     */
    var detailVerticesCount = 0

    /**
     * The detail mesh triangles; unsigned integers. [Size: 4 * #detailTriCount]
     */
    @Nullable
    var detailTris: ByteArray? = null

    /**
     * The number of triangles in the detail mesh.
     */
    var detailTriCount = 0

    /**
     * @name Off-Mesh Connections Attributes (Optional)
     * Used to define a custom point-to-point edge within the navigation graph, an
     * off-mesh connection is a user defined traversable connection made up to two vertices,
     * at least one of which resides within a navigation mesh polygon.
     * */

    /**
     * Off-mesh connection vertices. [(ax, ay, az, bx, by, bz) * #offMeshConCount]
     */
    @NotNull
    var offMeshConVertices: FloatArray = f0

    /**
     * Off-mesh connection radii. [Size: #offMeshConCount]
     */
    @NotNull
    var offMeshConRad: FloatArray = f0

    /**
     * User defined flags assigned to the off-mesh connections. [Size: #offMeshConCount]
     */
    @NotNull
    var offMeshConFlags: IntArray = i0

    /**
     * User defined area ids assigned to the off-mesh connections. [Size: #offMeshConCount]
     */
    @NotNull
    var offMeshConAreas: IntArray = i0

    /**
     * The permitted travel direction of the off-mesh connections. [Size: #offMeshConCount]
     * 0 = Travel only from endpoint A to endpoint B. Bidirectional travel.
     * */
    @NotNull
    var offMeshConDir: IntArray = i0

    /** The user defined ids of the off-mesh connection. [Size: #offMeshConCount] */
    @NotNull
    var offMeshConUserID: IntArray = i0

    /** The number of off-mesh connections. [Limit: >= 0] */
    var offMeshConCount = 0

    /** @name Tile Attributes
     * @note The tile grid/layer data can be left at zero if the destination is a single tile mesh.
     */

    /**
     * The user defined id of the tile.
     */
    var userId = 0

    /**
     * The tile's x-grid location within the multi-tile destination mesh. (Along the x-axis.)
     */
    var tileX = 0

    /**
     * The tile's y-grid location within the multi-tile desitation mesh. (Along the z-axis.)
     */
    var tileZ = 0

    /**
     * The tile's layer within the layered destination mesh. [Limit: >= 0] (Along the y-axis.)
     */
    var tileLayer = 0

    /**
     * The bounds of the tile. [(x, y, z)]
     * */
    lateinit var bmin: Vector3f
    lateinit var bmax: Vector3f

    /** General Configuration Attributes */

    /**
     * The agent height.
     */
    var walkableHeight = 0f

    /**
     * The agent radius.
     */
    var walkableRadius = 0f

    /**
     * The agent maximum traversable ledge. (Up/Down)
     */
    var walkableClimb = 0f

    /**
     * The xz-plane cell size of the polygon mesh. [Limit: > 0]
     */
    var cellSize = 0f

    /**
     * The y-axis cell height of the polygon mesh. [Limit: > 0]
     */
    var cellHeight = 0f

    /**
     * True if a bounding volume tree should be built for the tile.
     * @note The BVTree is not normally needed for layered navigation meshes.
     */
    var buildBvTree = false

}