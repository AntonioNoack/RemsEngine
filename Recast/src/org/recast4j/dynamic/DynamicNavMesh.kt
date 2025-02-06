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

import org.recast4j.LongHashMap
import org.recast4j.detour.NavMesh
import org.recast4j.detour.NavMeshParams
import org.recast4j.dynamic.collider.Collider
import org.recast4j.dynamic.io.VoxelFile
import org.recast4j.dynamic.io.VoxelTile
import org.recast4j.recast.AreaModification
import org.recast4j.recast.Heightfield
import org.recast4j.recast.RecastBuilder
import org.recast4j.recast.RecastBuilder.RecastBuilderResult
import org.recast4j.recast.Telemetry
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.math.floor

class DynamicNavMesh(voxelFile: VoxelFile) {

    val config = DynamicNavMeshConfig(voxelFile.useTiles, voxelFile.tileSizeX, voxelFile.tileSizeZ, voxelFile.cellSize)

    private val builder = RecastBuilder()
    private val tiles = LongHashMap<DynamicTile>()
    private val telemetry = Telemetry()
    private val navMeshParams = NavMeshParams()
    private val updateQueue: Queue<UpdateQueueItem> = LinkedBlockingQueue()
    private val currentColliderId = AtomicLong()
    private var dirty = true

    var navMesh: NavMesh? = null

    init {
        config.walkableHeight = voxelFile.walkableHeight
        config.walkableRadius = voxelFile.walkableRadius
        config.walkableClimb = voxelFile.walkableClimb
        config.walkableSlopeAngle = voxelFile.walkableSlopeAngle
        config.maxSimplificationError = voxelFile.maxSimplificationError
        config.maxEdgeLen = voxelFile.maxEdgeLen
        config.minRegionArea = voxelFile.minRegionArea
        config.regionMergeArea = voxelFile.regionMergeArea
        config.verticesPerPoly = voxelFile.verticesPerPoly
        config.buildDetailMesh = voxelFile.buildMeshDetail
        config.detailSampleDistance = voxelFile.detailSampleDistance
        config.detailSampleMaxError = voxelFile.detailSampleMaxError
        navMeshParams.origin.set(voxelFile.bounds)
        navMeshParams.tileWidth = voxelFile.cellSize * voxelFile.tileSizeX
        navMeshParams.tileHeight = voxelFile.cellSize * voxelFile.tileSizeZ
        navMeshParams.maxPolys = 0x8000
        for (t in voxelFile.tiles) {
            tiles[lookupKey(t.tileX.toLong(), t.tileZ.toLong())] = DynamicTile(t)
        }
    }
    /**
     * Voxel queries require checkpoints to be enabled in [DynamicNavMeshConfig]
     */
    fun voxelQuery(): VoxelQuery {
        return VoxelQuery(
            navMeshParams.origin,
            navMeshParams.tileWidth,
            navMeshParams.tileHeight
        ) { x: Int, z: Int -> lookupHeightfield(x, z) }
    }

    private fun lookupHeightfield(x: Int, z: Int): Heightfield? {
        return getTileAt(x, z)?.checkpoint?.heightfield
    }

    fun addCollider(collider: Collider): Long {
        val cid = currentColliderId.incrementAndGet()
        updateQueue.add(AddColliderQueueItem(cid, collider, getTiles(collider.bounds())))
        return cid
    }

    fun removeCollider(colliderId: Long) {
        updateQueue.add(RemoveColliderQueueItem(colliderId, getTilesByCollider(colliderId)))
    }

    /**
     * Perform full build of the nav mesh
     */
    fun build(walkableAreaModification: AreaModification) {
        processQueue()
        rebuild(tiles.values, walkableAreaModification)
    }

    /**
     * Perform incremental update of the nav mesh
     */
    fun update(walkableAreaModification: AreaModification): Boolean {
        return rebuild(processQueue(), walkableAreaModification)
    }

    private fun rebuild(stream: Collection<DynamicTile>, walkableAreaModification: AreaModification): Boolean {
        stream.forEach(Consumer { tile: DynamicTile -> this.rebuild(tile, walkableAreaModification) })
        return updateNavMesh()
    }

    private fun processQueue(): Collection<DynamicTile> {
        return consumeQueue().stream().peek { process(it) }
            .flatMap { i: UpdateQueueItem ->
                i.affectedTiles()
                    .stream()
            }.collect(Collectors.toSet())
    }

    private fun consumeQueue(): List<UpdateQueueItem> {
        val items: MutableList<UpdateQueueItem> = ArrayList()
        while (true) {
            val item = updateQueue.poll() ?: break
            items.add(item)
        }
        return items
    }

    private fun process(item: UpdateQueueItem) {
        for (tile in item.affectedTiles()) {
            item.process(tile)
        }
    }

    /**
     * Perform full build concurrently using the given [ExecutorService]
     */
    fun build(executor: ExecutorService, walkableAreaModification: AreaModification): CompletableFuture<Boolean> {
        processQueue()
        return rebuild(tiles.values, executor, walkableAreaModification)
    }

    /**
     * Perform incremental update concurrently using the given [ExecutorService]
     */
    fun update(executor: ExecutorService, walkableAreaModification: AreaModification): CompletableFuture<Boolean> {
        return rebuild(processQueue(), executor, walkableAreaModification)
    }

    private fun rebuild(
        tiles: Collection<DynamicTile>,
        executor: ExecutorService,
        walkableAreaModification: AreaModification
    ): CompletableFuture<Boolean> {
        return CompletableFuture.allOf(
            *(tiles.map { tile ->
                CompletableFuture.runAsync(
                    { rebuild(tile, walkableAreaModification) }, executor
                )
            }.toTypedArray())
        ).thenApply { updateNavMesh() }
    }

    private fun getTiles(bounds: FloatArray?): Collection<DynamicTile> {
        if (bounds == null) {
            return tiles.values
        }
        val minx = floor(((bounds[0] - navMeshParams.origin.x) / navMeshParams.tileWidth)).toInt()
        val minz = floor(((bounds[2] - navMeshParams.origin.z) / navMeshParams.tileHeight)).toInt()
        val maxx = floor(((bounds[3] - navMeshParams.origin.x) / navMeshParams.tileWidth)).toInt()
        val maxz = floor(((bounds[5] - navMeshParams.origin.z) / navMeshParams.tileHeight)).toInt()
        val tiles: MutableList<DynamicTile> = ArrayList()
        for (z in minz..maxz) {
            for (x in minx..maxx) {
                val tile = getTileAt(x, z)
                if (tile != null) {
                    tiles.add(tile)
                }
            }
        }
        return tiles
    }

    private fun getTilesByCollider(cid: Long): Collection<DynamicTile> {
        return tiles.values.filter { t -> t.containsCollider(cid) }
    }

    private fun rebuild(tile: DynamicTile, walkableAreaModification: AreaModification) {
        // NavMeshDataCreateParams params = new NavMeshDataCreateParams();
        // params.walkableHeight = config.walkableHeight;
        dirty = dirty or tile.build(builder, config, telemetry, walkableAreaModification)
    }

    private fun updateNavMesh(): Boolean {
        if (dirty) {
            val navMesh = NavMesh(navMeshParams, MAX_VERTICES_PER_POLY)
            tiles.forEachValue { it.addTo(navMesh) }
            this.navMesh = navMesh
            dirty = false
            return true
        }
        return false
    }

    private fun getTileAt(x: Int, z: Int): DynamicTile? {
        return tiles[lookupKey(x.toLong(), z.toLong())]
    }

    private fun lookupKey(x: Long, z: Long): Long {
        return z shl 32 or x
    }

    fun voxelTiles(): List<VoxelTile> {
        return tiles.values.map { it.voxelTile }
    }

    fun recastResults(): List<RecastBuilderResult?> {
        return tiles.values.map { it.recastResult }
    }

    companion object {
        const val MAX_VERTICES_PER_POLY = 6
    }
}