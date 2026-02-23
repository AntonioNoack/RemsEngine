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

import org.joml.AABBf
import org.recast4j.recast.geom.ConvexVolumeProvider
import org.recast4j.recast.geom.InputGeomProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

class RecastBuilder(val progressListener: RecastBuilderProgressListener? = null) {

    fun buildTiles(geom: InputGeomProvider, cfg: RecastConfig, executor: Executor?): List<RecastBuilderResult> {
        val bounds = geom.bounds
        val tw = Recast.calcTileCountX(bounds, cfg.cellSize, cfg.tileSizeX)
        val th = Recast.calcTileCountY(bounds, cfg.cellSize, cfg.tileSizeZ)
        return if (executor != null) buildMultiThread(geom, cfg, bounds, tw, th, executor)
        else buildSingleThread(geom, cfg, bounds, tw, th)
    }

    private fun buildSingleThread(
        geom: InputGeomProvider, cfg: RecastConfig, bounds: AABBf,
        tw: Int, th: Int
    ): List<RecastBuilderResult> {
        val result = ArrayList<RecastBuilderResult>(tw * th)
        val progressCtr = AtomicInteger()
        repeat(th) { y ->
            repeat(tw) { x ->
                result.add(buildTile(geom, cfg, bounds, x, y, progressCtr, tw * th))
            }
        }
        return result
    }

    private fun buildMultiThread(
        geom: InputGeomProvider, cfg: RecastConfig, bounds: AABBf,
        tw: Int, th: Int, executor: Executor
    ): List<RecastBuilderResult> {
        val result = ArrayList<RecastBuilderResult>(tw * th)
        val progressCtr = AtomicInteger()
        val latch = CountDownLatch(tw * th)
        repeat(th) { y ->
            repeat(tw) { x ->
                executor.execute {
                    try {
                        val tile = buildTile(geom, cfg, bounds, x, y, progressCtr, tw * th)
                        synchronized(result) { result.add(tile) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    latch.countDown()
                }
            }
        }
        latch.await()
        return result
    }

    private fun buildTile(
        geom: InputGeomProvider, cfg: RecastConfig, bounds: AABBf, tx: Int,
        ty: Int, counter: AtomicInteger, total: Int
    ): RecastBuilderResult {
        val result = build(geom, RecastBuilderConfig(cfg, bounds, tx, ty))
        progressListener?.onProgress(counter.incrementAndGet(), total)
        return result
    }

    fun build(geom: InputGeomProvider, builderCfg: RecastBuilderConfig): RecastBuilderResult {
        val cfg = builderCfg.cfg
        val ctx = Telemetry()
        //
        // Step 1. Rasterize input polygon soup.
        //
        val solid = RecastVoxelization.buildSolidHeightfield(geom, builderCfg, ctx)
        return build(builderCfg.tileX, builderCfg.tileZ, geom, cfg, solid, ctx)
    }

    fun build(
        tileX: Int, tileZ: Int, geom: ConvexVolumeProvider?, cfg: RecastConfig, solid: Heightfield,
        ctx: Telemetry?
    ): RecastBuilderResult {

        filterHeightfield(solid, cfg, ctx)

        val chf = buildCompactHeightfield(geom, cfg, ctx, solid)
        // Partition the heightfield so that we can use simple algorithm later
        // to triangulate the walkable areas.
        when (cfg.partitionType) {
            PartitionType.WATERSHED -> {
                // Prepare for region partitioning, by calculating distance field
                // along the walkable surface.
                RecastRegion.buildDistanceField(ctx, chf)
                // Partition the walkable surface into simple regions without holes.
                RecastRegion.buildRegions(ctx, chf, cfg.minRegionArea, cfg.mergeRegionArea)
            }
            PartitionType.MONOTONE -> {
                // Partition the walkable surface into simple regions without holes.
                // Monotone partitioning does not need distance field.
                RecastRegion.buildRegionsMonotone(ctx, chf, cfg.minRegionArea, cfg.mergeRegionArea)
            }
            PartitionType.LAYERS -> {
                // Partition the walkable surface into simple regions without holes.
                RecastRegion.buildLayerRegions(ctx, chf, cfg.minRegionArea)
            }
        }

        //
        // Step 5. Trace and simplify region contours.
        //

        // Create contours.
        val cset = RecastContour.buildContours(
            ctx, chf, cfg.maxSimplificationError, cfg.maxEdgeLen,
            RecastConstants.RC_CONTOUR_TESS_WALL_EDGES
        )

        //
        // Step 6. Build polygons mesh from contours.
        //
        val pmesh = RecastMesh.buildPolyMesh(ctx, cset, cfg.maxVerticesPerPoly)

        //
        // Step 7. Create detail mesh, which allows to access approximate height on each polygon.
        //
        val meshDetail = if (cfg.buildMeshDetail) {
            RecastMeshDetail.buildPolyMeshDetail(ctx, pmesh, chf, cfg.detailSampleDist, cfg.detailSampleMaxError)
        } else null
        return RecastBuilderResult(tileX, tileZ, solid, chf, cset, pmesh, meshDetail, ctx)
    }

    /**
     * Step 2. Filter walkable surfaces.
     */
    private fun filterHeightfield(solid: Heightfield, cfg: RecastConfig, ctx: Telemetry?) {
        // Once all geometry is rasterized, we do initial pass of filtering to
        // remove unwanted overhangs caused by the conservative rasterization
        // as well as filter spans where the character cannot possibly stand.
        if (cfg.filterLowHangingObstacles) {
            RecastFilter.filterLowHangingWalkableObstacles(ctx, cfg.walkableClimb, solid)
        }
        if (cfg.filterLedgeSpans) {
            RecastFilter.filterLedgeSpans(ctx, cfg.walkableHeight, cfg.walkableClimb, solid)
        }
        if (cfg.filterWalkableLowHeightSpans) {
            RecastFilter.filterWalkableLowHeightSpans(ctx, cfg.walkableHeight, solid)
        }
    }

    /** Step 3. Partition walkable surface to simple regions.  */
    private fun buildCompactHeightfield(
        volumeProvider: ConvexVolumeProvider?, cfg: RecastConfig, ctx: Telemetry?,
        solid: Heightfield
    ): CompactHeightfield {
        // Compact the heightfield so that it is faster to handle from now on.
        // This will result more cache coherent data as well as the neighbours
        // between walkable cells will be calculated.
        val chf = RecastCompact.buildCompactHeightfield(ctx, cfg.walkableHeight, cfg.walkableClimb, solid)

        // Erode the walkable area by agent radius.
        RecastArea.erodeWalkableArea(ctx, cfg.walkableRadius, chf)
        // (Optional) Mark areas.
        if (volumeProvider != null) {
            for (vol in volumeProvider.convexVolumes) {
                RecastArea.markConvexPolyArea(ctx, vol.verticesXZ, vol.minH, vol.maxH, vol.areaMod, chf)
            }
        }
        return chf
    }

    fun buildLayers(geom: InputGeomProvider, builderCfg: RecastBuilderConfig): Array<HeightfieldLayer>? {
        val ctx = Telemetry()
        val solid = RecastVoxelization.buildSolidHeightfield(geom, builderCfg, ctx)
        filterHeightfield(solid, builderCfg.cfg, ctx)
        val chf = buildCompactHeightfield(geom, builderCfg.cfg, ctx, solid)
        return RecastLayers.buildHeightfieldLayers(ctx, chf, builderCfg.cfg.walkableHeight)
    }
}