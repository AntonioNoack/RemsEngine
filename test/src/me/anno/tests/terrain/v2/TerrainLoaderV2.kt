package me.anno.tests.terrain.v2

import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.v2.TriTerrainChunk
import me.anno.ecs.components.mesh.terrain.v2.TriTerrainRenderer
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.ui.render.RenderView
import me.anno.io.files.FileReference
import me.anno.utils.assertions.assertNull
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.promise
import me.anno.utils.callbacks.F2F
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.structures.lists.Lists.all2
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Vector2i
import org.joml.Vector3f
import kotlin.math.log2

class TerrainLoaderV2(val height: F2F) : Component(), OnUpdate {

    companion object {
        private val LOGGER = LogManager.getLogger(TerrainLoaderV2::class)

        fun getChildIDs(parentId: VirtualChunkId): List<VirtualChunkId> {
            val xi = parentId.xi shl 1
            val zi = parentId.zi shl 1
            val lod = parentId.lod + 1
            return listOf(
                VirtualChunkId(xi, zi, lod),
                VirtualChunkId(xi, zi + 1, lod),
                VirtualChunkId(xi + 1, zi, lod),
                VirtualChunkId(xi + 1, zi + 1, lod)
            )
        }

        fun getParentID(id: VirtualChunkId): VirtualChunkId {
            return VirtualChunkId(id.xi shr 1, id.zi shr 1, id.lod - 1)
        }

        fun createChunk(id: VirtualChunkId, mesh: Mesh): VirtualChunk {
            val parentId = getParentID(id)
            return VirtualChunk(parentId, getChildIDs(parentId), mesh)
        }
    }

    data class VirtualChunkId(val xi: Int, val zi: Int, val lod: Int)
    class VirtualChunk(val parentId: VirtualChunkId, val siblingIds: List<VirtualChunkId>, val mesh: Mesh)

    val chunks = HashMap<VirtualChunkId, VirtualChunk>()

    @DebugProperty
    val size get() = chunks.size

    val numLods = 8

    val tileResolution = Vector2i(20, 20)

    /**
     * add a small overlapping region on the sides, so we don't need skirts
     * */
    val dsFactor = 0.5f / (tileResolution.x - 1f)

    var playerPosition = Vector3f()
        set(value) {
            field.set(value)
        }

    var lodBias = 13.5f

    fun getIdealLod(bounds: AABBf): Float {
        val distance = bounds.distance(playerPosition)
        return lodBias - log2(distance + 1f)
    }

    private fun getScale(lod: Int): Float {
        val lodI = numLods - 1 - lod
        return 10f * (1 shl lodI).toFloat()
    }

    private val terrain: TriTerrainChunk?
        get() = getComponent(TriTerrainRenderer::class)?.terrain

    fun init(x0: Int, z0: Int, x1: Int, z1: Int) {
        val terrain = terrain ?: return
        for (zi in z0 until z1) {
            for (xi in x0 until x1) {
                load(VirtualChunkId(xi, zi, 0), terrain)
            }
        }
    }

    fun load(id: VirtualChunkId, terrain: TriTerrainChunk) {
        val scale = getScale(id.lod)
        val ds = scale * dsFactor
        val x0 = scale * id.xi
        val y0 = scale * id.zi
        val x1 = scale * (id.xi + 1)
        val y1 = scale * (id.zi + 1)
        val bounds = AABBf(
            x0 - ds, 0f, y0 - ds,
            x1 + ds, 0f, y1 + ds
        )
        val mesh = terrain.createTile(bounds, tileResolution, height)
        assertNull(chunks.put(id, createChunk(id, mesh)))
    }

    private fun unload(id: VirtualChunkId, terrain: TriTerrainChunk) {
        val vChunk = chunks.remove(id) ?: return
        assertTrue(terrain.remove(vChunk.mesh))
        terrain.owner.remove(vChunk.mesh, true)
    }

    private fun split(id: VirtualChunkId, terrain: TriTerrainChunk) {
        unload(id, terrain)
        val childIds = getChildIDs(id)
        for (i in childIds.indices) {
            load(childIds[i], terrain)
        }
    }

    private fun join(chunk: VirtualChunk, terrain: TriTerrainChunk): Boolean {
        val allChildIds = chunk.siblingIds
        return if (allChildIds.all2 { it in chunks }) {
            // replace them :3
            for (i in allChildIds.indices) {
                unload(allChildIds[i], terrain)
            }
            load(chunk.parentId, terrain)
            true
        } else false
    }

    override fun onUpdate() {
        val terrain = terrain ?: return

        // update player position
        val rv = RenderView.currentInstance
        if (rv != null) playerPosition.set(rv.cameraPosition)

        // todo option to spawn new LOD=0-chunks
        for ((id, chunk) in chunks) {
            val lod = id.lod.toFloat()
            val idealLod = getIdealLod(chunk.mesh.getBounds())
            if (idealLod > lod + 1f && id.lod + 1 < numLods) {
                // make lod bigger -> make tile smaller -> split into children
                split(id, terrain)
                // chunks was touched -> iterator became unusable
                break
            } else if (idealLod < lod - 1f && id.lod > 0) {
                // make lod smaller -> make tile larger -> switch to parent
                if (join(chunk, terrain)) {
                    // chunks was touched -> iterator became unusable
                    break
                }
            }
        }
    }

    @DebugAction
    fun clearTerrain() {
        chunks.clear()
        terrain?.owner?.clear(true)
    }

    @DebugAction
    fun loadTerrain() {
        val terrain = terrain ?: return
        val file = getStorageFile()
        if (!file.exists) {
            LOGGER.warn("$file is missing, cannot load terrain")
            return
        }
        promise { callback ->
            file.readBytes(callback)
        }.then { bytes ->
            clearTerrain()
            val ok = TerrainSerialization.loadTerrain(chunks, bytes, terrain)
            LOGGER.info("Loaded terrain, ok? $ok, ${bytes.size.formatFileSize()}")
        }
    }

    @DebugAction
    fun saveTerrain() {
        val file = getStorageFile()
        TerrainSerialization.saveTerrain(chunks, file)
    }

    private fun getStorageFile(): FileReference {
        return workspace.getChild("terrain-v2.bin")
    }
}
