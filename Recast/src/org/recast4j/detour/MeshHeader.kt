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

import org.joml.Vector3f

/** Provides high level information related to a dtMeshTile object.  */
open class MeshHeader {

    /** Tile magic number. (Used to identify the data format.)  */
    var magic = 0

    /** Tile data format version number.  */
    var version = 0

    /** The x-position of the tile within the dtNavMesh tile grid. (x, y, layer)  */
    var x = 0

    /** The y-position of the tile within the dtNavMesh tile grid. (x, y, layer)  */
    var y = 0

    /** The layer of the tile within the dtNavMesh tile grid. (x, y, layer)  */
    var layer = 0

    /** The user defined id of the tile.  */
    var userId = 0

    /** The number of polygons in the tile.  */
    var polyCount = 0

    /** The number of vertices in the tile.  */
    var vertCount = 0

    /** The number of allocated links.  */
    var maxLinkCount = 0

    /** The number of sub-meshes in the detail mesh.  */
    var detailMeshCount = 0

    /** The number of unique vertices in the detail mesh. (In addition to the polygon vertices.)  */
    var detailVertCount = 0

    /** The number of triangles in the detail mesh.  */
    var detailTriCount = 0

    /** The number of bounding volume nodes. (Zero if bounding volumes are disabled.)  */
    var bvNodeCount = 0

    /** The number of off-mesh connections.  */
    var offMeshConCount = 0

    /** The index of the first polygon which is an off-mesh connection.  */
    var offMeshBase = 0

    /** The height of the agents using the tile.  */
    var walkableHeight = 0f

    /** The radius of the agents using the tile.  */
    var walkableRadius = 0f

    /** The maximum climb height of the agents using the tile.  */
    var walkableClimb = 0f

    /** The minimum bounds of the tile's AABB.  */
    val bmin = Vector3f()

    /** The maximum bounds of the tile's AABB. [(x, y, z)]  */
    val bmax = Vector3f()

    /** The bounding volume quantization factor.  */
    var bvQuantizationFactor = 0f

    companion object {
        /** A magic number used to detect compatibility of navigation tile data.  */
        const val DT_NAVMESH_MAGIC = 'D'.code shl 24 or ('N'.code shl 16) or ('A'.code shl 8) or 'V'.code

        /** A version number used to detect compatibility of navigation tile data.  */
        const val DT_NAVMESH_VERSION = 7
        const val DT_NAVMESH_VERSION_RECAST4J_FIRST = 0x8807
        const val DT_NAVMESH_VERSION_RECAST4J_NO_POLY_FIRSTLINK = 0x8808
        const val DT_NAVMESH_VERSION_RECAST4J_32BIT_BVTREE = 0x8809
        const val DT_NAVMESH_VERSION_RECAST4J_LAST = 0x8809

        /** A magic number used to detect the compatibility of navigation tile states.  */
        const val DT_NAVMESH_STATE_MAGIC = 'D'.code shl 24 or ('N'.code shl 16) or ('M'.code shl 8) or 'S'.code

        /** A version number used to detect compatibility of navigation tile states.  */
        const val DT_NAVMESH_STATE_VERSION = 1
    }

}