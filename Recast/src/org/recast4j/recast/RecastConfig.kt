/*
Copyright (c) 2009-2010 Mikko Mononen memon@inside.org
recast4j Copyright (c) 2015-2019 Piotr Piastucki piotr@jtilia.org

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

import org.recast4j.recast.PartitionType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

class RecastConfig(
    val useTiles: Boolean,
    /** The width/depth size of tile's on the xz-plane. [Limit: &gt;= 0] [Units: vx]  */
    val tileSizeX: Int,
    val tileSizeZ: Int,
    /** The size of the non-navigable border around the heightfield. [Limit: &gt;=0] [Units: vx]  */
    val borderSize: Int,
    val partitionType: PartitionType,
    /** The xz-plane cell size to use for fields. [Limit: &gt; 0] [Units: wu]  */
    val cellSize: Float,
    /** The y-axis cell size to use for fields. [Limit: &gt; 0] [Units: wu]  */
    val cellHeight: Float,
    /** The maximum slope that is considered walkable. [Limits: 0 &lt;= value &lt; 90] [Units: Degrees]  */
    val walkableSlopeAngle: Float,
    filterLowHangingObstacles: Boolean,
    filterLedgeSpans: Boolean,
    filterWalkableLowHeightSpans: Boolean,
    agentHeight: Float,
    agentRadius: Float,
    agentMaxClimb: Float,
    minRegionArea: Float,
    mergeRegionArea: Float,
    edgeMaxLen: Float,
    edgeMaxError: Float,
    verticesPerPoly: Int,
    buildMeshDetail: Boolean,
    detailSampleDist: Float,
    detailSampleMaxError: Float,
    walkableAreaMod: AreaModification
) {
    /**
     * Minimum floor to 'ceiling' height that will still allow the floor area to be considered walkable. [Limit: &gt;= 3]
     * [Units: vx]
     */
    val walkableHeight: Int

    /** Maximum ledge height that is considered to still be traversable. [Limit: &gt;=0] [Units: vx]  */
    val walkableClimb: Int

    /**
     * The distance to erode/shrink the walkable area of the heightfield away from obstructions. [Limit: &gt;=0] [Units:
     * vx]
     */
    val walkableRadius: Int

    /** The maximum allowed length for contour edges along the border of the mesh. [Limit: &gt;=0] [Units: vx]  */
    val maxEdgeLen: Int

    /**
     * The maximum distance a simplfied contour's border edges should deviate the original raw contour. [Limit: &gt;=0]
     * [Units: vx]
     */
    val maxSimplificationError: Float

    /** The minimum number of cells allowed to form isolated island areas. [Limit: &gt;=0] [Units: vx]  */
    val minRegionArea: Int

    /**
     * Any regions with a span count smaller than this value will, if possible, be merged with larger regions. [Limit:
     * &gt;=0] [Units: vx]
     */
    val mergeRegionArea: Int

    /**
     * The maximum number of vertices allowed for polygons generated during the contour to polygon conversion process.
     * [Limit: &gt;= 3]
     */
    val maxVerticesPerPoly: Int

    /**
     * Sets the sampling distance to use when generating the detail mesh. (For height detail only.) [Limits: 0 or >=
     * 0.9] [Units: wu]
     */
    val detailSampleDist: Float

    /**
     * The maximum distance the detail mesh surface should deviate from heightfield data. (For height detail only.)
     * [Limit: &gt;=0] [Units: wu]
     */
    val detailSampleMaxError: Float
    val walkableAreaMod: AreaModification
    val filterLowHangingObstacles: Boolean
    val filterLedgeSpans: Boolean
    val filterWalkableLowHeightSpans: Boolean

    /** Set to false to disable building detailed mesh  */
    val buildMeshDetail: Boolean

    /** Set of original settings passed in world units  */
    val minRegionAreaWorld: Float
    val mergeRegionAreaWorld: Float
    val walkableHeightWorld: Float
    val walkableClimbWorld: Float
    val walkableRadiusWorld: Float
    val maxEdgeLenWorld: Float

    /**
     * Non-tiled build configuration
     */
    constructor(
        partitionType: PartitionType, cellSize: Float, cellHeight: Float, agentHeight: Float, agentRadius: Float,
        agentMaxClimb: Float, agentMaxSlope: Float, regionMinSize: Int, regionMergeSize: Int, edgeMaxLen: Float,
        edgeMaxError: Float, verticesPerPoly: Int, detailSampleDist: Float, detailSampleMaxError: Float,
        walkableAreaMod: AreaModification
    ) : this(
        partitionType,
        cellSize,
        cellHeight,
        agentMaxSlope,
        true,
        true,
        true,
        agentHeight,
        agentRadius,
        agentMaxClimb,
        regionMinSize,
        regionMergeSize,
        edgeMaxLen,
        edgeMaxError,
        verticesPerPoly,
        detailSampleDist,
        detailSampleMaxError,
        walkableAreaMod,
        true
    ) {
    }

    /**
     * Non-tiled build configuration
     */
    constructor(
        partitionType: PartitionType,
        cellSize: Float,
        cellHeight: Float,
        agentMaxSlope: Float,
        filterLowHangingObstacles: Boolean,
        filterLedgeSpans: Boolean,
        filterWalkableLowHeightSpans: Boolean,
        agentHeight: Float,
        agentRadius: Float,
        agentMaxClimb: Float,
        regionMinSize: Int,
        regionMergeSize: Int,
        edgeMaxLen: Float,
        edgeMaxError: Float,
        verticesPerPoly: Int,
        detailSampleDist: Float,
        detailSampleMaxError: Float,
        walkableAreaMod: AreaModification,
        buildMeshDetail: Boolean
    ) : this(
        false, 0, 0, 0,
        partitionType, cellSize, cellHeight, agentMaxSlope,
        filterLowHangingObstacles, filterLedgeSpans, filterWalkableLowHeightSpans,
        agentHeight, agentRadius, agentMaxClimb,
        regionMinSize * regionMinSize * cellSize * cellSize,
        regionMergeSize * regionMergeSize * cellSize * cellSize,
        edgeMaxLen, edgeMaxError, verticesPerPoly, buildMeshDetail,
        detailSampleDist, detailSampleMaxError, walkableAreaMod
    ) {
        // Note: area = size*size in [Units: wu]
    }

    init {
        walkableHeight = ceil((agentHeight / cellHeight)).toInt()
        walkableHeightWorld = agentHeight
        walkableClimb = floor((agentMaxClimb / cellHeight)).toInt()
        walkableClimbWorld = agentMaxClimb
        walkableRadius = ceil((agentRadius / cellSize)).toInt()
        walkableRadiusWorld = agentRadius
        this.minRegionArea = round(minRegionArea / (cellSize * cellSize)).toInt()
        minRegionAreaWorld = minRegionArea
        this.mergeRegionArea = round(mergeRegionArea / (cellSize * cellSize)).toInt()
        mergeRegionAreaWorld = mergeRegionArea
        maxEdgeLen = (edgeMaxLen / cellSize).toInt()
        maxEdgeLenWorld = edgeMaxLen
        maxSimplificationError = edgeMaxError
        maxVerticesPerPoly = verticesPerPoly
        this.detailSampleDist = if (detailSampleDist < 0.9f) 0f else cellSize * detailSampleDist
        this.detailSampleMaxError = cellHeight * detailSampleMaxError
        this.walkableAreaMod = walkableAreaMod
        this.filterLowHangingObstacles = filterLowHangingObstacles
        this.filterLedgeSpans = filterLedgeSpans
        this.filterWalkableLowHeightSpans = filterWalkableLowHeightSpans
        this.buildMeshDetail = buildMeshDetail
    }

    companion object {
        fun calcBorder(agentRadius: Float, cs: Float): Int {
            return 3 + ceil((agentRadius / cs)).toInt()
        }
    }
}