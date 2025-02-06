/*
recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.dynamic

import org.recast4j.detour.MeshData
import org.recast4j.detour.NavMesh
import org.recast4j.detour.NavMeshBuilder.createNavMeshData
import org.recast4j.detour.NavMeshDataCreateParams
import org.recast4j.detour.NavMeshDataCreateParams.Companion.f0
import org.recast4j.detour.NavMeshDataCreateParams.Companion.i0
import org.recast4j.dynamic.collider.Collider
import org.recast4j.dynamic.io.VoxelTile
import org.recast4j.recast.*
import org.recast4j.recast.RecastBuilder.RecastBuilderResult
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class DynamicTile(val voxelTile: VoxelTile) {

    var checkpoint: DynamicTileCheckpoint? = null
    var recastResult: RecastBuilderResult? = null
    var meshData: MeshData? = null

    private val colliders = ConcurrentHashMap<Long, Collider?>()
    private var dirty = true
    private var id = 0L

    fun build(
        builder: RecastBuilder,
        config: DynamicNavMeshConfig,
        telemetry: Telemetry,
        walkableAreaModification: AreaModification
    ): Boolean {
        if (dirty) {
            val heightfield = buildHeightfield(config, telemetry)
            val r = buildRecast(builder, config, voxelTile, heightfield, telemetry, walkableAreaModification)
            val params = navMeshCreateParams(
                voxelTile.tileX, voxelTile.tileZ, voxelTile.cellSize,
                voxelTile.cellHeight, config, r
            )
            meshData = createNavMeshData(params)
            return true
        }
        return false
    }

    private fun buildHeightfield(config: DynamicNavMeshConfig, telemetry: Telemetry): Heightfield {
        val rasterizedColliders: Collection<Long> = if (checkpoint != null) checkpoint!!.colliders else emptySet()
        val heightfield = if (checkpoint != null) checkpoint!!.heightfield else voxelTile.heightfield()
        colliders.forEach { (id, c) ->
            if (!rasterizedColliders.contains(id)) {
                heightfield.bmax.y = max(heightfield.bmax.y, c!!.bounds()[4] + heightfield.cellHeight * 2)
                c.rasterize(heightfield, telemetry)
            }
        }
        if (config.enableCheckpoints) {
            checkpoint = DynamicTileCheckpoint(heightfield, HashSet(colliders.keys))
        }
        return heightfield
    }

    private fun buildRecast(
        builder: RecastBuilder, config: DynamicNavMeshConfig, vt: VoxelTile,
        heightfield: Heightfield, telemetry: Telemetry, walkableAreaModification: AreaModification
    ): RecastBuilderResult {
        val rcConfig = RecastConfig(
            config.useTiles,
            config.tileSizeX,
            config.tileSizeZ,
            vt.borderSize,
            config.partitionType,
            vt.cellSize,
            vt.cellHeight,
            config.walkableSlopeAngle,
            true,
            true,
            true,
            config.walkableHeight,
            config.walkableRadius,
            config.walkableClimb,
            config.minRegionArea,
            config.regionMergeArea,
            config.maxEdgeLen,
            config.maxSimplificationError,
            min(DynamicNavMesh.MAX_VERTICES_PER_POLY, config.verticesPerPoly),
            true,
            config.detailSampleDistance,
            config.detailSampleMaxError,
            walkableAreaModification
        )
        val r = builder.build(vt.tileX, vt.tileZ, null, rcConfig, heightfield, telemetry)
        if (config.keepIntermediateResults) {
            recastResult = r
        }
        return r
    }

    fun addCollider(cid: Long, collider: Collider?) {
        colliders[cid] = collider
        dirty = true
    }

    fun containsCollider(cid: Long): Boolean {
        return colliders.containsKey(cid)
    }

    fun removeCollider(colliderId: Long) {
        if (colliders.remove(colliderId) != null) {
            dirty = true
            checkpoint = null
        }
    }

    private fun navMeshCreateParams(
        tilex: Int, tileZ: Int, cellSize: Float, cellHeight: Float,
        config: DynamicNavMeshConfig, rcResult: RecastBuilderResult
    ): NavMeshDataCreateParams {
        val mesh = rcResult.mesh
        val meshDetail = rcResult.meshDetail
        val params = NavMeshDataCreateParams()
        for (i in 0 until mesh.numPolygons) {
            mesh.flags[i] = 1
        }
        params.tileX = tilex
        params.tileZ = tileZ
        params.vertices = mesh.vertices
        params.vertCount = mesh.numVertices
        params.polys = mesh.polygons
        params.polyAreas = mesh.areaIds
        params.polyFlags = mesh.flags
        params.polyCount = mesh.numPolygons
        params.maxVerticesPerPolygon = mesh.maxVerticesPerPolygon
        if (meshDetail != null) {
            params.detailMeshes = meshDetail.subMeshes
            params.detailVertices = meshDetail.vertices
            params.detailVerticesCount = meshDetail.numVertices
            params.detailTris = meshDetail.triangles
            params.detailTriCount = meshDetail.numTriangles
        }
        params.walkableHeight = config.walkableHeight
        params.walkableRadius = config.walkableRadius
        params.walkableClimb = config.walkableClimb
        params.bmin = mesh.bmin
        params.bmax = mesh.bmax
        params.cellSize = cellSize
        params.cellHeight = cellHeight
        params.buildBvTree = true
        params.offMeshConCount = 0
        params.offMeshConRad = f0
        params.offMeshConVertices = params.offMeshConRad
        params.offMeshConUserID = i0
        params.offMeshConFlags = params.offMeshConUserID
        params.offMeshConAreas = params.offMeshConFlags
        params.offMeshConDir = params.offMeshConAreas
        return params
    }

    fun addTo(navMesh: NavMesh) {
        val meshData = meshData
        id = if (meshData != null) {
            navMesh.addTile(meshData, 0, 0)
        } else {
            navMesh.removeTile(id)
            0
        }
    }
}