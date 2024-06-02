package me.anno.ecs.components.mesh.unique

import me.anno.cache.ICacheData
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.graph.hdb.allocator.AllocationManager
import me.anno.utils.types.size
import me.anno.utils.Clock
import me.anno.utils.Logging.hash32
import me.anno.utils.assertions.assertTrue
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3d
import org.lwjgl.opengl.GL14C.glMultiDrawArrays
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

/**
 * renderer for static geometry, that still can be partially loaded/unloaded
 *
 * all instances must use the same material (for now),
 * but we do support fully custom MeshVertexData
 *
 * todo somehow use indexed meshes (less load on the vertex shader)
 * */
abstract class UniqueMeshRenderer<Mesh : IMesh, Key>(
    val attributes: List<Attribute>,
    override val vertexData: MeshVertexData,
    val drawMode: DrawMode
) : MeshSpawner(), IMesh, ICacheData, AllocationManager<MeshEntry<Mesh>, StaticBuffer> {

    abstract fun getData(key: Key, mesh: Mesh): StaticBuffer?

    abstract fun forEachHelper(key: Key, transform: Transform): Material?

    /**
     * defines what the world looks like for Raycasting,
     * and for AABBs
     * */
    override fun forEachMesh(run: (IMesh, Material?, Transform) -> Unit) {
        var i = 0
        for ((key, entry) in entryLookup) {
            val transform = getTransform(i++)
            val material = forEachHelper(key, transform)
            run(entry.mesh!!, material, transform)
        }
    }

    val stride = attributes.sumOf { it.byteSize }

    val entryLookup = HashMap<Key, MeshEntry<Mesh>>()
    val entries = ArrayList<MeshEntry<Mesh>>()
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

    val clock = Clock(LOGGER)

    fun set(key: Key, entry: MeshEntry<Mesh>): Boolean {
        val old = entryLookup[key]
        if (old != null) remove(key, entry.mesh != old.mesh)
        return add(key, entry)
    }

    fun add(key: Key, entry: MeshEntry<Mesh>): Boolean {
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

    fun remove(key: Key, deleteMesh: Boolean): Boolean {
        val entry = entryLookup.remove(key) ?: return false
        assertTrue(remove(entry, entries, ranges))
        numPrimitives -= entry.buffer.vertexCount
        if (deleteMesh) {
            entry.mesh?.destroy()
        }
        entry.buffer.destroy()
        invalidate()
        return true
    }

    var isValid = true

    var baseShape: Buffer? = null

    fun invalidate() {
        isValid = false
    }

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
        this.clickId = clickId
        pipeline.addMesh(this, this, entity)
        return clickId + 1
    }

    private fun push(buffer: StaticBuffer, start: Int, end: Int) {
        if (start >= end) return
        if (tmpLengths.position() == tmpLengths.capacity()) {
            finish(buffer)
        }
        tmpStarts.put(start)
        tmpLengths.put(end - start)
    }

    private fun finish(buffer: StaticBuffer) {
        if (tmpLengths.position() > 0) {
            tmpStarts.flip()
            tmpLengths.flip()
            glMultiDrawArrays(buffer.drawMode.id, tmpStarts, tmpLengths)
            tmpStarts.clear()
            tmpLengths.clear()
        }
    }

    override fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean) {
        if (numPrimitives == 0L) return
        val buffer = buffer0
        if (!buffer.isUpToDate) {
            LOGGER.warn("Buffer ${hash32(buffer)} isn't ready")
            return
        }
        GFXState.bind()
        buffer.drawLength = numPrimitives.toInt()
        buffer.bind(shader)
        val frustum = pipeline?.frustum
        var currStart = 0
        var currEnd = 0
        for (i in entries.indices) {
            val entry = entries[i]
            if (frustum == null || frustum.isVisible(entry.bounds)) { // frustum culling
                val range = entry.range
                if (range.first != currEnd) {
                    push(buffer, currStart, currEnd)
                    currStart = range.first
                }
                currEnd = range.last + 1
            }
        }
        push(buffer, currStart, currEnd)
        finish(buffer)
        buffer.unbind()
    }

    override fun drawInstanced(
        pipeline: Pipeline, shader: Shader, materialIndex: Int,
        instanceData: Buffer, drawLines: Boolean
    ) {
        LOGGER.warn("Drawing a bulk-mesh instanced doesn't make sense")
    }

    override fun getRange(key: MeshEntry<Mesh>): IntRange {
        return key.range
    }

    override fun allocate(newSize: Int): StaticBuffer {
        val buffer = buffer1
        buffer.vertexCount = newSize
        val clock = Clock(LOGGER)
        buffer.uploadEmpty(newSize.toLong() * stride)
        clock.stop("UploadEmpty")
        return buffer
    }

    override fun roundUpStorage(requiredSize: Int): Int {
        return requiredSize * 2
    }

    override fun copy(key: MeshEntry<Mesh>, from: Int, fromData: StaticBuffer, to: IntRange, toData: StaticBuffer) {
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

    companion object {
        private val LOGGER = LogManager.getLogger(UniqueMeshRenderer::class)
        private fun createBuffer(): IntBuffer {
            val buffer = ByteBuffer.allocateDirect(4096 * 4)
            buffer.order(ByteOrder.nativeOrder())
            return buffer.asIntBuffer()
        }

        private val tmpStarts = createBuffer()
        private val tmpLengths = createBuffer()
    }
}