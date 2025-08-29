package me.anno.ecs.components.mesh.unique

import me.anno.cache.ICacheData
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.CompactAttributeLayout
import me.anno.gpu.buffer.StaticBuffer
import me.anno.graph.hdb.allocator.AllocationManager
import me.anno.utils.Clock
import me.anno.utils.InternalAPI
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Ranges.size
import org.apache.logging.log4j.LogManager

abstract class UMRData<Key, Mesh>(attributes: CompactAttributeLayout) :
    AllocationManager<Mesh, StaticBuffer, Mesh>, ICacheData {

    companion object {
        private val LOGGER = LogManager.getLogger(UMRData::class)
    }

    @InternalAPI
    val entries = HashMap<Key, Mesh>()

    @InternalAPI
    val sortedEntries = ArrayList<Mesh>()

    @InternalAPI
    val sortedRanges = ArrayList<IntRange>()

    /**
     * buffers are swapped when they need to be resized
     * */
    var buffer0 = StaticBuffer("umr0i", attributes, 0, BufferUsage.DYNAMIC)
    var buffer1 = StaticBuffer("umr1i", attributes, 0, BufferUsage.DYNAMIC)

    val buffer: StaticBuffer get() = buffer0
    val stride: Int get() = buffer0.stride

    private val clock = Clock(LOGGER)

    fun add(key: Key, mesh: Mesh): Boolean {
        val entries = entries
        if (key in entries) return false
        val b0 = buffer0
        val b1 = buffer1
        clock.start()
        val bx = insert(
            sortedEntries, sortedRanges,
            mesh, mesh,
            b0.vertexCount, b0,
        ).second
        clock.stop("Insert", 0.01)
        entries[key] = mesh
        assertTrue(bx === b0 || bx === b1)
        buffer1 = if (bx === b1) b0 else b1
        buffer0 = if (bx === b1) b1 else b0
        return true
    }

    fun remove(key: Key, destroyMesh: Boolean): Mesh? {
        val entry = entries.remove(key) ?: return null
        assertTrue(remove(entry, sortedEntries, sortedRanges))
        if (destroyMesh && entry is ICacheData) {
            entry.destroy()
        }
        return entry
    }

    fun clear(destroyMeshes: Boolean) {
        entries.clear()
        for (entry in sortedEntries) {
            if (destroyMeshes && entry is ICacheData) {
                entry.destroy()
            }
        }
        sortedEntries.clear()
        sortedRanges.clear()
    }

    override fun destroy() {
        buffer0.destroy()
        buffer1.destroy()
        for ((_, entry) in entries) {
            if (entry is ICacheData) {
                entry.destroy()
            }
        }
    }

    override fun allocate(newSize: Int): StaticBuffer {
        val buffer = buffer1
        val oldSize = buffer.vertexCount
        buffer.vertexCount = newSize
        if (newSize > 0 && newSize != oldSize) {
            val clock = Clock(LOGGER)
            LOGGER.info("Changing buffer size from $oldSize to $newSize")
            buffer.uploadEmpty(newSize.toLong() * stride)
            clock.stop("UploadEmpty")
        }
        return buffer
    }

    override fun deallocate(data: StaticBuffer) {
        // we just swap between buffer0 and buffer1, so we must not destroy anything
    }

    override fun allocationKeepsOldData(): Boolean = true
    override fun roundUpStorage(requiredSize: Int): Int =requiredSize * 2

    override fun moveData(from: Int, fromData: StaticBuffer, to: IntRange, toData: StaticBuffer) {
        // when this is executed for vertices, all responsible indices must be found and adjusted!
        // when this is executed for indices, all is fine
        fromData.copyElementsTo(toData, from.toLong(), to.first.toLong(), to.size.toLong())
    }
}