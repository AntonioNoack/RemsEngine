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
package org.recast4j.dynamic.io

import org.joml.AABBf
import org.joml.Vector3f
import org.recast4j.dynamic.DynamicNavMesh
import org.recast4j.recast.AreaModification
import org.recast4j.recast.PartitionType
import org.recast4j.recast.RecastBuilderResult
import org.recast4j.recast.RecastConfig
import java.nio.ByteOrder

class VoxelFile {

    var version = 0
    var partitionType: PartitionType = PartitionType.WATERSHED
    var filterLowHangingObstacles = true
    var filterLedgeSpans = true
    var filterWalkableLowHeightSpans = true
    var walkableRadius = 0f
    var walkableHeight = 0f
    var walkableClimb = 0f
    var walkableSlopeAngle = 0f
    var cellSize = 0f
    var maxSimplificationError = 0f
    var maxEdgeLen = 0f
    var minRegionArea = 0f
    var regionMergeArea = 0f
    var verticesPerPoly = 0
    var buildMeshDetail = false
    var detailSampleDistance = 0f
    var detailSampleMaxError = 0f
    var useTiles = false
    var tileSizeX = 0
    var tileSizeZ = 0
    var rotation = Vector3f()
    val bounds = AABBf()

    val tiles = ArrayList<VoxelTile>()
    fun addTile(tile: VoxelTile) {
        tiles.add(tile)
    }

    fun getConfig(
        tile: VoxelTile,
        partitionType: PartitionType,
        maxPolyVertices: Int,
        filterLowHangingObstacles: Boolean,
        filterLedgeSpans: Boolean,
        filterWalkableLowHeightSpans: Boolean,
        walkableAreaMod: AreaModification,
        buildMeshDetail: Boolean,
        detailSampleDist: Float,
        detailSampleMaxError: Float
    ): RecastConfig {
        return RecastConfig(
            useTiles,
            tileSizeX,
            tileSizeZ,
            tile.borderSize,
            partitionType,
            cellSize,
            tile.cellHeight,
            walkableSlopeAngle,
            filterLowHangingObstacles,
            filterLedgeSpans,
            filterWalkableLowHeightSpans,
            walkableHeight,
            walkableRadius,
            walkableClimb,
            minRegionArea,
            regionMergeArea,
            maxEdgeLen,
            maxSimplificationError,
            maxPolyVertices,
            buildMeshDetail,
            detailSampleDist,
            detailSampleMaxError,
            walkableAreaMod
        )
    }

    companion object {

        val PREFERRED_BYTE_ORDER: ByteOrder = ByteOrder.BIG_ENDIAN

        const val MAGIC = 'V'.code shl 24 or ('O'.code shl 16) or ('X'.code shl 8) or 'L'.code
        const val VERSION_EXPORTER_MASK = 0xF000
        const val VERSION_EXPORTER_RECAST4J = 0x1000

        fun from(config: RecastConfig, results: List<RecastBuilderResult>): VoxelFile {
            val f = VoxelFile()
            f.version = 1
            f.partitionType = config.partitionType
            f.filterLowHangingObstacles = config.filterLowHangingObstacles
            f.filterLedgeSpans = config.filterLedgeSpans
            f.filterWalkableLowHeightSpans = config.filterWalkableLowHeightSpans
            f.walkableRadius = config.walkableRadiusWorld
            f.walkableHeight = config.walkableHeightWorld
            f.walkableClimb = config.walkableClimbWorld
            f.walkableSlopeAngle = config.walkableSlopeAngle
            f.cellSize = config.cellSize
            f.maxSimplificationError = config.maxSimplificationError
            f.maxEdgeLen = config.maxEdgeLenWorld
            f.minRegionArea = config.minRegionAreaWorld
            f.regionMergeArea = config.mergeRegionAreaWorld
            f.verticesPerPoly = config.maxVerticesPerPoly
            f.buildMeshDetail = config.buildMeshDetail
            f.detailSampleDistance = config.detailSampleDist
            f.detailSampleMaxError = config.detailSampleMaxError
            f.useTiles = config.useTiles
            f.tileSizeX = config.tileSizeX
            f.tileSizeZ = config.tileSizeZ
            f.bounds.clear()
            for (r in results) {
                f.tiles.add(VoxelTile(r.tileX, r.tileZ, r.solidHeightField))
                f.bounds.union(r.solidHeightField.bounds)
            }
            return f
        }

        fun from(mesh: DynamicNavMesh): VoxelFile {
            val f = VoxelFile()
            f.version = 1
            val config = mesh.config
            f.partitionType = config.partitionType
            f.filterLowHangingObstacles = config.filterLowHangingObstacles
            f.filterLedgeSpans = config.filterLedgeSpans
            f.filterWalkableLowHeightSpans = config.filterWalkableLowHeightSpans
            f.walkableRadius = config.walkableRadius
            f.walkableHeight = config.walkableHeight
            f.walkableClimb = config.walkableClimb
            f.walkableSlopeAngle = config.walkableSlopeAngle
            f.cellSize = config.cellSize
            f.maxSimplificationError = config.maxSimplificationError
            f.maxEdgeLen = config.maxEdgeLen
            f.minRegionArea = config.minRegionArea
            f.regionMergeArea = config.regionMergeArea
            f.verticesPerPoly = config.verticesPerPoly
            f.buildMeshDetail = config.buildDetailMesh
            f.detailSampleDistance = config.detailSampleDistance
            f.detailSampleMaxError = config.detailSampleMaxError
            f.useTiles = config.useTiles
            f.tileSizeX = config.tileSizeX
            f.tileSizeZ = config.tileSizeZ
            f.bounds.clear()
            for (tile in mesh.voxelTiles()) {
                val heightfield = tile.heightfield()
                f.tiles.add(VoxelTile(tile.tileX, tile.tileZ, heightfield))
                f.bounds.union(tile.bounds)
            }
            return f
        }
    }
}