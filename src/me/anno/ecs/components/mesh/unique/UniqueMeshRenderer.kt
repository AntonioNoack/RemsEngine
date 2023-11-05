package me.anno.ecs.components.mesh.unique

import me.anno.cache.ICacheData
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.*
import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.graph.hdb.allocator.AllocationManager
import me.anno.graph.hdb.allocator.size
import me.anno.io.files.FileReference
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL31C.*

/**
 * todo Mesh, but each one is only rendered once,
 *  and all use the same material -> can be rendered in a single draw call
 * */
abstract class UniqueMeshRenderer<Key>(
    val attributes: List<Attribute>,
    override val vertexData: MeshVertexData,
    material: Material,
    val drawMode: DrawMode
) : MeshSpawner(), IMesh, ICacheData,
    AllocationManager<MeshEntry, StaticBuffer> {

    abstract fun getData(key: Key, mesh: Mesh): StaticBuffer?

    override val materials: List<FileReference> = listOf(material.ref)
    override val numMaterials: Int get() = 1

    val stride = attributes.sumOf { it.byteSize }


    val entryLookup = HashMap<Key, MeshEntry>()
    val entries = ArrayList<MeshEntry>()

    private var buffer0 = StaticBuffer("umr0", attributes, 0, GL_DYNAMIC_DRAW)
    private var buffer1 = StaticBuffer("urm1", attributes, 0, GL_DYNAMIC_DRAW)

    override var numPrimitives: Long = 0

    override fun ensureBuffer() {
        // not really anything to do for now...
    }

    val clock = Clock()

    fun set(key: Key, entry: MeshEntry): Boolean {
        val old = entryLookup[key]
        if (old != null) remove(key)
        return add(key, entry)
    }

    fun add(key: Key, entry: MeshEntry): Boolean {
        if (key in entryLookup) return false
        val b0 = buffer0
        val b1 = buffer1
        clock.start()
        val bx = insert(
            entries, entry, entry.buffer,
            entry.range, b0.vertexCount, b0,
            false
        ).second
        clock.stop("Insert", 0.01)
        entryLookup[key] = entry
        entries.add(entry)
        numPrimitives += entry.buffer.vertexCount
        if (bx !== b0 && bx !== b1) throw IllegalArgumentException()
        this.buffer1 = if (bx === b1) b0 else b1
        this.buffer0 = if (bx === b1) b1 else b0
        invalidateAABB()
        return true
    }

    fun remove(key: Key): Boolean {
        val entry = entryLookup.remove(key) ?: return false
        entries.remove(entry)
        numPrimitives -= entry.buffer.vertexCount
        entry.mesh?.destroy()
        entry.buffer.destroy()
        invalidate()
        return true
    }

    var isValid = true

    fun invalidate() {
        isValid = false
    }

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
        this.clickId = clickId
        pipeline.addMesh(this, this, entity)
        return clickId + 1
    }

    override fun draw(shader: Shader, materialIndex: Int, drawLines: Boolean) {
        if (numPrimitives == 0L) return
        val buffer = buffer0
        if (!buffer.isUpToDate) {
            LOGGER.warn("Buffer ${System.identityHashCode(buffer)} isn't ready")
            return
        }
        buffer.drawLength = numPrimitives.toInt()
        buffer.bind(shader)
        var i0 = 0
        var i1 = 0
        for (i in entries.indices) {
            val entry = entries[i].range
            if (entry.first != i1) {
                if (i1 > i0) {
                    buffer.draw(i0, i1 - i0)
                }
                i0 = entry.first
            }
            i1 = entry.last + 1
        }
        if (i1 > i0) {
            buffer.draw(i0, i1 - i0)
        }
        buffer.unbind()
    }

    override fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: Buffer) {
        throw NotImplementedError("Drawing a bulk-mesh instanced doesn't make sense")
    }

    override fun getRange(key: MeshEntry): IntRange {
        return key.range
    }

    override fun allocate(newSize: Int): StaticBuffer {
        val buffer = buffer1
        buffer.vertexCount = newSize
        val clock = Clock()
        buffer.uploadEmpty(newSize.toLong() * stride)
        clock.stop("UploadEmpty")
        return buffer
    }

    override fun roundUpStorage(requiredSize: Int): Int {
        return requiredSize * 2
    }

    override fun copy(key: MeshEntry, from: Int, fromData: StaticBuffer, to: IntRange, toData: StaticBuffer) {
        val fromData1 = key.buffer
        copy(0, fromData1, to, toData)
        key.range = to
    }

    override fun copy(from: Int, fromData: StaticBuffer, to: IntRange, toData: StaticBuffer) {
        if (to.size == 0) return
        if (from == to.first && fromData === toData) return

        GFX.check()
        fromData.ensureBuffer()
        toData.ensureBuffer()

        // println("copying from ${fromData.pointer} to ${toData.pointer}: from $from to $to / ${fromData.vertexCount}/${toData.vertexCount}")
        if (fromData.vertexCount < to.size) {
            LOGGER.warn("Illegal copy $from to $to, ${fromData.vertexCount}x / ${toData.vertexCount}x")
            return
        }

        if (fromData.pointer == 0 || toData.pointer == 0) {
            LOGGER.warn("Illegal data")
            return
        }

        glBindBuffer(GL_COPY_READ_BUFFER, fromData.pointer)
        glBindBuffer(GL_COPY_WRITE_BUFFER, toData.pointer)
        glCopyBufferSubData(
            GL_COPY_READ_BUFFER,
            GL_COPY_WRITE_BUFFER,
            from.toLong(),
            to.first.toLong() * stride,
            to.size.toLong() * stride
        )
        GFX.check()
    }

    override fun destroy() {
        buffer0.destroy()
        buffer1.destroy()
        for ((_, entry) in entryLookup) {
            entry.buffer.destroy()
            entry.mesh?.destroy()
        }
    }

    override val className: String
        get() = "UniqueMeshRenderer"

    companion object {
        private val LOGGER = LogManager.getLogger(UniqueMeshRenderer::class)
    }
}