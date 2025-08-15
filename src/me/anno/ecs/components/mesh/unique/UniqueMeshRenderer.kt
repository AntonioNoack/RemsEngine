package me.anno.ecs.components.mesh.unique

import me.anno.cache.ICacheData
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.render.Frustum
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.CompactAttributeLayout
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.graph.hdb.allocator.AllocationManager
import me.anno.maths.Maths.min
import me.anno.utils.Clock
import me.anno.utils.InternalAPI
import me.anno.utils.Logging.hash32
import me.anno.utils.assertions.assertTrue
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.size
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3
import org.lwjgl.opengl.GL46C.glMultiDrawArrays
import java.nio.IntBuffer
import kotlin.math.max

/**
 * renderer for static geometry, that still can be partially loaded/unloaded
 *
 * all instances must use the same material (for now),
 * but we do support fully custom MeshVertexData
 *
 * todo somehow use indexed meshes (less load on the vertex shader)
 * */
abstract class UniqueMeshRenderer<Key, Mesh>(
    val attributes: CompactAttributeLayout,
    override val vertexData: MeshVertexData,
    val drawMode: DrawMode
) : MeshSpawner(), IMesh, ICacheData, AllocationManager<Mesh, StaticBuffer, Mesh> {

    /**
     * Return transform and material for that specific mesh for traditional methods.
     * Rendering must handle transform and material without this function:
     * - transform must be encoded in the buffer somehow
     * - material is taken from UniqueMeshRender.cachedMaterials
     * */
    open fun getTransformAndMaterial(key: Key, transform: Transform): Material? = null

    @InternalAPI
    val entries = HashMap<Key, Mesh>()

    @InternalAPI
    val sortedEntries = ArrayList<Mesh>()

    @InternalAPI
    val sortedRanges = ArrayList<IntRange>()

    private var buffer0 = StaticBuffer("umr0", attributes, 0, BufferUsage.DYNAMIC)
    private var buffer1 = StaticBuffer("umr1", attributes, 0, BufferUsage.DYNAMIC)

    val stride: Int get() = attributes.stride
    val values: List<Mesh> get() = sortedEntries
    val buffer: StaticBuffer get() = buffer0

    @DebugProperty
    @NotSerializedProperty
    override var numPrimitives: Long = 0

    var totalNumPrimitives = 0L

    /**
     * If value > 1, your shader must load vertex attributes from the buffer on its own.
     * Override bindBuffer() to bind it.
     * */
    var verticesPerEntry: Int = 1
        set(value) {
            field = max(value, 1)
        }

    override fun ensureBuffer() {
        // not really anything to do for now...
    }

    open fun bindBuffer(shader: Shader, buffer: StaticBuffer) {
        buffer.bind(shader)
    }

    val clock = Clock(LOGGER)

    operator fun set(key: Key, entry: Mesh): Boolean {
        val prev = entries[key]
        if (prev != null) remove(key, entry != prev)
        return add(key, entry)
    }

    operator fun get(key: Key): Mesh? = entries[key]

    fun add(key: Key, entry: Mesh): Boolean {
        if (key in entries) return false
        val b0 = buffer0
        val b1 = buffer1
        clock.start()
        val bx = insert(
            sortedEntries, sortedRanges, entry, entry,
            b0.vertexCount, b0,
        ).second
        clock.stop("Insert", 0.01)
        entries[key] = entry
        totalNumPrimitives += getRange(entry).size
        assertTrue(bx === b0 || bx === b1)
        this.buffer1 = if (bx === b1) b0 else b1
        this.buffer0 = if (bx === b1) b1 else b0
        invalidateBounds()
        return true
    }

    fun remove(key: Key, destroyMesh: Boolean): Mesh? {
        val entry = this@UniqueMeshRenderer.entries.remove(key) ?: return null
        assertTrue(remove(entry, sortedEntries, sortedRanges))
        totalNumPrimitives -= getRange(entry).size
        if (destroyMesh && entry is ICacheData) {
            entry.destroy()
        }
        invalidate()
        return entry
    }

    var isValid = true

    fun invalidate() {
        isValid = false
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        clickId = pipeline.getClickId(this)
        pipeline.addMesh(this, this, transform)
    }

    private fun push(start: Int, end: Int) {
        if (start >= end) return
        if (tmpLengths.position() == tmpLengths.capacity()) {
            finish()
        }
        tmpStarts.put(start)
        tmpLengths.put(end - start)
    }

    private fun finish() {
        if (tmpLengths.position() > 0) {
            tmpStarts.flip()
            tmpLengths.flip()
            glMultiDrawArrays(drawMode.id, tmpStarts, tmpLengths)
            tmpStarts.clear()
            tmpLengths.clear()
        }
    }

    override fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean) {
        if (totalNumPrimitives == 0L) return
        val buffer = buffer0
        if (!buffer.isUpToDate) {
            LOGGER.warn("Buffer ${hash32(buffer)} isn't ready")
            return
        }
        var counter = 0L
        GFXState.bind()
        // doesn't matter as long as it's greater than zero; make it the actual value for debugging using DebugGPUStorage
        buffer.drawLength = max(min(totalNumPrimitives, Int.MAX_VALUE.toLong()).toInt(), 1)
        bindBuffer(shader, buffer)
        val factor = max(verticesPerEntry, 1)
        val frustum = pipeline?.frustum
        var transform = transform?.globalTransform
        // small optimization: most UniqueMeshRenderers will be at the origin
        if (transform != null && transform.isIdentity()) transform = null
        var currStart = 0
        var currEnd = 0
        for (i in sortedEntries.indices) {
            val entry = sortedEntries[i]
            if (shallRenderEntry(frustum, transform, entry)) {
                val range = getRange(entry)
                if (range.first != currEnd) {
                    push(currStart * factor, currEnd * factor)
                    counter += (currEnd - currStart) * factor
                    currStart = range.first
                }
                currEnd = range.last + 1
            }
        }
        push(currStart * factor, currEnd * factor)
        counter += (currEnd - currStart) * factor
        numPrimitives = when (drawMode) {
            DrawMode.POINTS -> counter
            DrawMode.LINES, DrawMode.LINE_STRIP -> counter shr 1
            DrawMode.TRIANGLES, DrawMode.TRIANGLE_STRIP -> counter / 3
        }
        finish()
        buffer.unbind()
    }

    abstract fun shallRenderEntry(frustum: Frustum?, transform: Matrix4x3?, entry: Mesh): Boolean

    override fun drawInstanced(
        pipeline: Pipeline, shader: Shader, materialIndex: Int,
        instanceData: Buffer, drawLines: Boolean
    ) {
        LOGGER.warn("Drawing a bulk-mesh instanced doesn't make sense")
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

    override fun roundUpStorage(requiredSize: Int): Int {
        return requiredSize * 2
    }

    override fun moveData(from: Int, fromData: StaticBuffer, to: IntRange, toData: StaticBuffer) {
        fromData.copyElementsTo(toData, from.toLong(), to.first.toLong(), to.size.toLong())
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

    open fun clear(destroyMeshes: Boolean) {
        entries.clear()
        for (entry in sortedEntries) {
            if (destroyMeshes && entry is ICacheData) {
                entry.destroy()
            }
        }
        sortedEntries.clear()
        sortedRanges.clear()
        numPrimitives = 0
    }

    companion object {
        private val LOGGER = LogManager.getLogger(UniqueMeshRenderer::class)
        private fun createBuffer(): IntBuffer {
            val tmpCapacity = 16 * 1024
            return ByteBufferPool.allocateDirect(tmpCapacity).asIntBuffer()
        }

        private val tmpStarts = createBuffer()
        private val tmpLengths = createBuffer()
    }
}