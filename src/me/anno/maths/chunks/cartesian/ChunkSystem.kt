package me.anno.maths.chunks.cartesian

import me.anno.cache.Promise
import me.anno.cache.CacheSection
import me.anno.ecs.Component
import me.anno.maths.chunks.PlayerLocation
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.utils.structures.lists.Lists.all2
import me.anno.utils.structures.lists.Lists.any2
import org.joml.Vector3d.Companion.lengthSquared
import org.joml.Vector3i
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * general chunk system,
 * LODs can be generated e.g., with OctTree
 * */
abstract class ChunkSystem<Chunk : Any, Element>(
    val bitsX: Int, val bitsY: Int, val bitsZ: Int
) : Component() {

    var timeoutMillis = 10_000L
    val chunks = CacheSection<Vector3i, Chunk>("Chunks")

    fun removeIf(condition: (key: Vector3i, value: Chunk) -> Boolean) {
        chunks.removeIf { key, value ->
            @Suppress("UNCHECKED_CAST")
            val value2 = value.value
            value2 != null && condition(key, value2)
        }
    }

    val sizeX = 1 shl bitsX
    val sizeY = 1 shl bitsY
    val sizeZ = 1 shl bitsZ

    val totalSize = 1 shl (bitsX + bitsY + bitsZ)

    val maskX = sizeX - 1
    val maskY = sizeY - 1
    val maskZ = sizeZ - 1

    val dx by lazy { getIndex(1, 0, 0) }
    val dy by lazy { getIndex(0, 1, 0) }
    val dz by lazy { getIndex(0, 0, 1) }

    abstract fun createChunk(
        chunkX: Int, chunkY: Int, chunkZ: Int,
        size: Int, result: Promise<Chunk>
    )

    abstract fun getElement(
        container: Chunk, localX: Int, localY: Int, localZ: Int,
        index: Int
    ): Element

    /**
     * returns whether the element was successfully set
     * */
    abstract fun setElement(
        container: Chunk, localX: Int, localY: Int, localZ: Int,
        index: Int, element: Element
    ): Boolean

    open fun getChunk(chunkX: Int, chunkY: Int, chunkZ: Int, generateIfMissing: Boolean): Promise<Chunk>? {
        val key = Vector3i(chunkX, chunkY, chunkZ)
        return if (generateIfMissing) {
            chunks.getEntry(key, timeoutMillis) { k, result ->
                createChunk(chunkX, chunkY, chunkZ, totalSize, result)
            }
        } else {
            chunks.getEntryWithoutGenerator(key)
        }
    }

    fun removeChunk(chunkX: Int, chunkY: Int, chunkZ: Int): Promise<Chunk>? {
        return chunks.removeEntry(Vector3i(chunkX, chunkY, chunkZ))
    }

    open fun getChunkAt(
        globalX: Double,
        globalY: Double,
        globalZ: Double,
        generateIfMissing: Boolean
    ): Promise<Chunk>? {
        val cx = floor(globalX).toInt() shr bitsX
        val cy = floor(globalY).toInt() shr bitsY
        val cz = floor(globalZ).toInt() shr bitsZ
        return getChunk(cx, cy, cz, generateIfMissing)
    }

    open fun getChunkAt(globalX: Int, globalY: Int, globalZ: Int, generateIfMissing: Boolean): Promise<Chunk>? {
        val cx = globalX shr bitsX
        val cy = globalY shr bitsY
        val cz = globalZ shr bitsZ
        return getChunk(cx, cy, cz, generateIfMissing)
    }

    open fun getElementAt(globalX: Int, globalY: Int, globalZ: Int, generateIfMissing: Boolean): Element? {
        val chunk = getChunkAt(globalX, globalY, globalZ, generateIfMissing)
            ?.waitFor(!generateIfMissing) ?: return null
        val lx = globalX and maskX
        val ly = globalY and maskY
        val lz = globalZ and maskZ
        return getElement(
            chunk, lx, ly, lz,
            getIndex(lx, ly, lz)
        )
    }

    open fun getElementAt(globalX: Int, globalY: Int, globalZ: Int): Element {
        val chunk = getChunkAt(globalX, globalY, globalZ, true)!!
            .waitFor("ChunkSystem.getElementAt")!!
        val lx = globalX and maskX
        val ly = globalY and maskY
        val lz = globalZ and maskZ
        return getElement(
            chunk, lx, ly, lz,
            getIndex(lx, ly, lz)
        )
    }

    open fun setElementAt(
        globalX: Int,
        globalY: Int,
        globalZ: Int,
        generateIfMissing: Boolean,
        element: Element
    ): Boolean {
        val chunk = getChunkAt(globalX, globalY, globalZ, generateIfMissing)
            ?.waitFor(!generateIfMissing) ?: return false
        val lx = globalX and maskX
        val ly = globalY and maskY
        val lz = globalZ and maskZ
        return setElement(
            chunk, lx, ly, lz,
            getIndex(lx, ly, lz),
            element
        )
    }

    open fun getIndex(localX: Int, localY: Int, localZ: Int): Int {
        return localX + (localZ + localY.shl(bitsZ)).shl(bitsX)
    }

    open fun decodeIndex(yzx: Int, dst: Vector3i = Vector3i()): Vector3i {
        dst.x = yzx and maskX
        val yz = yzx shr bitsX
        dst.z = yz and maskZ
        val y = yz shr bitsZ
        dst.y = y and maskY
        return dst
    }

    fun process(
        min: Vector3i,
        max: Vector3i,
        generateIfMissing: Boolean,
        processor: (x: Int, y: Int, z: Int, e: Element) -> Unit
    ) {
        // iterate in chunks
        // don't generate, if not generateIfMissing
        val minX = min.x shr bitsX
        val maxX = (max.x + sizeX - 1) shr bitsX
        val minY = min.y shr bitsY
        val maxY = (max.y + sizeY - 1) shr bitsY
        val minZ = min.z shr bitsZ
        val maxZ = (max.z + sizeZ - 1) shr bitsZ
        for (cy in minY..maxY) {
            for (cz in minZ..maxZ) {
                for (cx in minX..maxX) {
                    val chunk = getChunk(cx, cy, cz, generateIfMissing)
                        ?.waitFor(!generateIfMissing)
                    if (chunk != null) {
                        val baseX = cx shl bitsX
                        val baseY = cy shl bitsY
                        val baseZ = cz shl bitsZ
                        val minX2 = max(min.x - baseX, 0)
                        val maxX2 = min(max.x - baseX, sizeX)
                        val minY2 = max(min.y - baseY, 0)
                        val maxY2 = min(max.y - baseY, sizeY)
                        val minZ2 = max(min.z - baseZ, 0)
                        val maxZ2 = min(max.z - baseZ, sizeZ)
                        for (ly in minY2 until maxY2) {
                            val gy = ly + baseY
                            for (lz in minZ2 until maxZ2) {
                                val gz = lz + baseZ
                                var yzxIndex = getIndex(minX2, ly, lz)
                                for (lx in minX2 until maxX2) {
                                    val element = getElement(chunk, lx, ly, lz, yzxIndex++)
                                    processor(lx + baseX, gy, gz, element)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param loadingDistance in this radius, new chunks are created
     * @param unloadingDistance outside this radius, chunks are destroyed
     * @param players basis for loading & unloading chunks
     * */
    fun updateVisibility(
        loadingDistance: Double,
        unloadingDistance: Double,
        players: List<PlayerLocation>
    ) {
        // ensure every player is inside a chunk
        for (player in players) {
            getChunkAt(player.x, player.y, player.z, true)
        }
        // load chunks, which need to be loaded, because they are close
        // unload chunks, which no longer need to be loaded
        // use sth like k nearest neighbors for this (??)...
        synchronized(chunks) {
            val sx2 = sizeX * 0.5
            val sy2 = sizeY * 0.5
            val sz2 = sizeZ * 0.5
            val unloadingSq = unloadingDistance * unloadingDistance
            val loadingSq = loadingDistance * loadingDistance
            removeIf { pos, chunk ->
                val px = (pos.x shl bitsX) + sx2
                val py = (pos.y shl bitsY) + sy2
                val pz = (pos.z shl bitsZ) + sz2
                val shallRemove = players.all2 {
                    val dist = lengthSquared(px - it.x, py - it.y, pz - it.z)
                    dist * it.unloadMultiplier > unloadingSq
                }
                shallRemove
            }
            for (pos in chunks.cache.keys.toList()) { // toList() to copy the entries, so we don't get ConcurrentModificationExceptions
                val px = (pos.x shl bitsX) + sx2
                val py = (pos.y shl bitsY) + sy2
                val pz = (pos.z shl bitsZ) + sz2
                for (side in BlockSide.entries) {
                    val dx = side.x
                    val dy = side.y
                    val dz = side.z
                    val cx = px + dx * sizeX
                    val cy = py + dy * sizeY
                    val cz = pz + dz * sizeZ
                    if (players.any2 { p ->
                            val dist = lengthSquared(cx - p.x, cy - p.y, cz - p.z)
                            dist * p.loadMultiplier < loadingSq
                        }) {
                        getChunk(pos.x + dx, pos.y + dy, pos.z + dz, true)
                    }
                }
            }
        }
    }

    override fun destroy() {
        super.destroy()
        removeIf { _, _ -> true }
    }
}