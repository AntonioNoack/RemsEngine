package me.anno.ecs.components.mesh.unique

import me.anno.cache.ICacheData
import me.anno.ecs.Entity
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.graph.hdb.allocator.AllocationManager
import me.anno.graph.hdb.allocator.size
import me.anno.io.files.FileReference
import me.anno.utils.Clock
import me.anno.utils.Logging.hash32
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3d

/**
 * renderer for static geometry, that still can be partially loaded/unloaded
 *
 * all instances must use the same material (for now),
 * but we do support fully custom MeshVertexData
 * */
abstract class UniqueMeshRenderer<Key>(
    val attributes: List<Attribute>,
    override val vertexData: MeshVertexData,
    material: Material, val drawMode: DrawMode
) : MeshSpawner(), IMesh, ICacheData, AllocationManager<MeshEntry, StaticBuffer> {

    abstract fun getData(key: Key, mesh: Mesh): StaticBuffer?

    override val materials: List<FileReference> = listOf(material.ref)
    override val numMaterials: Int get() = 1

    val stride = attributes.sumOf { it.byteSize }

    val entryLookup = HashMap<Key, MeshEntry>()
    val entries = ArrayList<MeshEntry>()
    val ranges = ArrayList<IntRange>()

    private var buffer0 = StaticBuffer("umr0", attributes, 0, BufferUsage.DYNAMIC)
    private var buffer1 = StaticBuffer("urm1", attributes, 0, BufferUsage.DYNAMIC)

    @DebugProperty
    @NotSerializedProperty
    override var numPrimitives: Long = 0

    override fun ensureBuffer() {
        // not really anything to do for now...
    }

    private val boundsF = AABBf()
    override fun getBounds(): AABBf = boundsF

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        // calculate local aabb
        val local = boundsF
        local.clear()
        for (i in entries.indices) {
            val entry = entries[i]
            local.union(entry.bounds)
        }
        localAABB.set(local)

        // calculate global aabb
        val global = globalAABB
        local.transform(globalTransform, global)

        // add the result to the output
        aabb.union(global)
        return true
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
            entries, ranges, entry, entry.buffer,
            entry.range, b0.vertexCount, b0,
            false
        ).second
        clock.stop("Insert", 0.01)
        entryLookup[key] = entry
        numPrimitives += entry.buffer.vertexCount
        if (bx !== b0 && bx !== b1) throw IllegalArgumentException()
        this.buffer1 = if (bx === b1) b0 else b1
        this.buffer0 = if (bx === b1) b1 else b0
        invalidateAABB()
        return true
    }

    fun remove(key: Key): Boolean {
        val entry = entryLookup.remove(key) ?: return false
        if (true) {
            // todo entries are sorted, so use binary search to remove it
            // todo we also need to remove this section from ranges
            entries.remove(entry)
            ranges.clear()
            ranges.addAll(compact(entries))
        }
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
            LOGGER.warn("Buffer ${hash32(buffer)} isn't ready")
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

    override fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: Buffer, drawLines: Boolean) {
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
        fromData.copyElementsTo(toData, from.toLong(), to.first.toLong(), to.size.toLong())
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