package me.anno.ecs.components.chunks.cartesian

import me.anno.ecs.components.chunks.PlayerLocation
import org.joml.Vector3d.lengthSquared
import org.joml.Vector3i
import kotlin.math.max
import kotlin.math.min

/**
 * general chunk system,
 * LODs can be generated e.g. with OctTree
 * */
abstract class ChunkSystem<Chunk, Element>(
    val bitsX: Int,
    val bitsY: Int,
    val bitsZ: Int,
    initialCapacity: Int = 256
) : Iterable<MutableMap.MutableEntry<Vector3i, Chunk>> {

    val chunks = HashMap<Vector3i, Chunk>(initialCapacity)

    val sizeX = 1 shl bitsX
    val sizeY = 1 shl bitsY
    val sizeZ = 1 shl bitsZ

    val totalSize = 1 shl (bitsX + bitsY + bitsZ)

    val maskX = sizeX - 1
    val maskY = sizeY - 1
    val maskZ = sizeZ - 1

    abstract fun createChunk(
        chunkX: Int, chunkY: Int, chunkZ: Int,
        size: Int
    ): Chunk

    abstract fun getElement(
        container: Chunk, localX: Int, localY: Int, localZ: Int,
        yzxIndex: Int
    ): Element

    abstract fun setElement(
        container: Chunk, localX: Int, localY: Int, localZ: Int,
        yzxIndex: Int, element: Element
    ): Boolean

    fun getChunk(chunkX: Int, chunkY: Int, chunkZ: Int, generateIfMissing: Boolean): Chunk? {
        val key = Vector3i(chunkX, chunkY, chunkZ)
        return if (generateIfMissing) {
            synchronized(chunks) {
                chunks.getOrPut(key) {
                    val chunk = createChunk(chunkX, chunkY, chunkZ, totalSize)
                    onCreateChunk(chunk, chunkX, chunkY, chunkZ)
                    chunk
                }
            }
        } else chunks[key]
    }

    fun getChunkAt(globalX: Double, globalY: Double, globalZ: Double, generateIfMissing: Boolean): Chunk? {
        val cx = globalX.toInt() shr bitsX
        val cy = globalY.toInt() shr bitsY
        val cz = globalZ.toInt() shr bitsZ
        return getChunk(cx, cy, cz, generateIfMissing)
    }

    fun getChunkAt(globalX: Int, globalY: Int, globalZ: Int, generateIfMissing: Boolean): Chunk? {
        val cx = globalX shr bitsX
        val cy = globalY shr bitsY
        val cz = globalZ shr bitsZ
        return getChunk(cx, cy, cz, generateIfMissing)
    }

    fun getElementAt(globalX: Int, globalY: Int, globalZ: Int, generateIfMissing: Boolean): Element? {
        val chunk = getChunkAt(globalX, globalY, globalZ, generateIfMissing) ?: return null
        val lx = globalX and maskX
        val ly = globalY and maskY
        val lz = globalZ and maskZ
        return getElement(
            chunk, lx, ly, lz,
            getYZXIndex(lx, ly, lz)
        )
    }

    fun setElementAt(globalX: Int, globalY: Int, globalZ: Int, generateIfMissing: Boolean, element: Element): Boolean {
        val chunk = getChunkAt(globalX, globalY, globalZ, generateIfMissing) ?: return false
        val lx = globalX and maskX
        val ly = globalY and maskY
        val lz = globalZ and maskZ
        return setElement(
            chunk, lx, ly, lz,
            getYZXIndex(lx, ly, lz),
            element
        )
    }

    fun getYZXIndex(localX: Int, localY: Int, localZ: Int): Int {
        return localX + (localZ + localY.shl(bitsZ)).shl(bitsX)
    }

    fun decodeYZXIndex(yzx: Int, dst: Vector3i = Vector3i()): Vector3i {
        dst.x = yzx and maskX
        val yz = yzx shr bitsX
        dst.z = yz and maskZ
        val y = yz shr bitsZ
        dst.y = y and maskY
        return dst
    }

    // todo we might want a function, where we process neighbors as well, e.g. 3x3x3 or 5x5x5, or 1x3x1 or sth like that

    fun process(
        min: Vector3i,
        max: Vector3i,
        generateIfMissing: Boolean,
        processor: (x: Int, y: Int, z: Int, e: Element) -> Unit
    ) {
        // iterate in chunks
        // don't generate, if not generateIfMissing
        // todo currently this might only work for positive values, idk
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
                    if (chunk != null) {
                        val baseX = cx shl bitsX
                        val baseY = cy shl bitsY
                        val baseZ = cz shl bitsZ
                        val minX2 = max(min.x, baseX)
                        val maxX2 = min(max.x, baseX + sizeX)
                        val minY2 = max(min.y, baseY)
                        val maxY2 = min(max.y, baseY + sizeY)
                        val minZ2 = max(min.z, baseZ)
                        val maxZ2 = min(max.z, baseZ + sizeZ)
                        for (y in minY2 until maxY2) {
                            for (z in minZ2 until maxZ2) {
                                var yzxIndex = getYZXIndex(minX2, y, z)
                                for (x in minX2 until maxX2) {
                                    val element = getElement(chunk, x, y, z, yzxIndex++)
                                    processor(x, y, z, element)
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
            chunks.entries.removeIf { (pos, chunk) ->
                val px = (pos.x shl bitsX) + sx2
                val py = (pos.y shl bitsY) + sy2
                val pz = (pos.z shl bitsZ) + sz2
                val shallRemove = players.all {
                    val dist = lengthSquared(px - it.x, py - it.y, pz - it.z)
                    dist * it.unloadMultiplier > unloadingDistance
                }
                if (shallRemove) onDestroyChunk(chunk, pos.x, pos.y, pos.z)
                shallRemove
            }
            val sides = listOf(
                Vector3i(-1, 0, 0),
                Vector3i(+1, 0, 0),
                Vector3i(0, -1, 0),
                Vector3i(0, +1, 0),
                Vector3i(0, 0, -1),
                Vector3i(0, 0, +1),
            )
            for ((pos, _) in chunks) {
                val px = (pos.x shl bitsX) + sx2
                val py = (pos.y shl bitsY) + sy2
                val pz = (pos.z shl bitsZ) + sz2
                for ((dx, dy, dz) in sides) {
                    val cx = px + dx * sizeX
                    val cy = py + dy * sizeY
                    val cz = pz + dz * sizeZ
                    if (players.any { p ->
                            val dist = lengthSquared(cx - p.x, cy - p.y, cz - p.z)
                            dist * p.loadMultiplier < loadingDistance
                        }) {
                        getChunk(pos.x + dx, pos.y + dy, pos.z + dz, true)
                    }
                }
            }
        }
    }

    open fun onCreateChunk(chunk: Chunk, chunkX: Int, chunkY: Int, chunkZ: Int) {}

    open fun onDestroyChunk(chunk: Chunk, chunkX: Int, chunkY: Int, chunkZ: Int) {}

    override fun iterator() = chunks.iterator()

    operator fun Vector3i.component1() = x
    operator fun Vector3i.component2() = y
    operator fun Vector3i.component3() = z

}