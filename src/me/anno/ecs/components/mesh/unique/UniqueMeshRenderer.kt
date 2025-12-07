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
import me.anno.gpu.buffer.CompactAttributeLayout
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.GPUBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.maths.MinMax.min
import me.anno.utils.Clock
import me.anno.utils.Logging.hash32
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.types.Ranges.size
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3
import org.lwjgl.PointerBuffer
import org.lwjgl.opengl.GL15C.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL46C.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL46C.glMultiDrawArrays
import org.lwjgl.opengl.GL46C.glMultiDrawElements
import java.nio.IntBuffer
import kotlin.math.max

/**
 * renderer for static geometry, that still can be partially loaded/unloaded;
 * e.g., this is ideal for rendering irregular terrain meshes
 *
 * all instances must use the same material (for now),
 * but we do support fully custom MeshVertexData
 * */
abstract class UniqueMeshRenderer<Key, Mesh>(
    val attributes: CompactAttributeLayout,
    override val vertexData: MeshVertexData,
    indexedRendering: Boolean,
    val drawMode: DrawMode
) : MeshSpawner(), IMesh, ICacheData {

    /**
     * Return transform and material for that specific mesh for traditional methods.
     * Rendering must handle transform and material without this function:
     * - transform must be encoded in the buffer somehow
     * - material is taken from UniqueMeshRender.cachedMaterials
     * */
    open fun getTransformAndMaterial(key: Key, transform: Transform): Material? = null

    val umrIndexData = if (indexedRendering) UMRIndexData(this) else null
    val umrVertexData = UMRVertexData(this)

    abstract fun getVertexRange(mesh: Mesh): IntRange
    abstract fun setVertexRange(mesh: Mesh, value: IntRange)

    abstract fun getIndexRange(mesh: Mesh): IntRange
    abstract fun setIndexRange(mesh: Mesh, value: IntRange)

    abstract fun insertVertexData(from: Int, fromData: Mesh, to: IntRange, toData: StaticBuffer)
    abstract fun insertIndexData(from: Int, fromData: Mesh, to: IntRange, toData: StaticBuffer)

    val stride: Int get() = attributes.stride
    val values: List<Mesh> get() = umrVertexData.sortedEntries
    val buffer: StaticBuffer get() = umrVertexData.buffer

    @DebugProperty
    @NotSerializedProperty
    override var numPrimitives: Long = 0

    var totalNumPrimitives = 0L

    /**
     * If value > 1, your shader must load vertex attributes from the buffer on its own.
     * Override bindBuffer() to bind it.
     *
     * Not tested with indices yet.
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
        val prev = umrVertexData.entries[key]
        if (prev != null) remove(key, entry != prev)
        return add(key, entry)
    }

    operator fun get(key: Key): Mesh? = umrVertexData.entries[key]

    fun add(key: Key, mesh: Mesh): Boolean {
        val added = umrVertexData.add(key, mesh)
        if (added) {
            umrIndexData?.add(key, mesh)
            totalNumPrimitives += getVertexRange(mesh).size
            invalidateBounds()
        }
        return added
    }

    fun remove(key: Key, destroyMesh: Boolean): Mesh? {
        val removed = umrVertexData.remove(key, destroyMesh)
        if (removed != null) {
            umrIndexData?.remove(key, destroyMesh)
            totalNumPrimitives -= getVertexRange(removed).size
            invalidate()
        }
        return removed
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
        if (tmpLengths.position() == DRAW_CAPACITY) {
            finish()
        }
        if (umrIndexData != null) {
            // *4, because this is in bytes, and our indices are 4 bytes each
            tmpStartsI.put(start * 4L)
        } else {
            tmpStarts.put(start)
        }
        tmpLengths.put(end - start)
    }

    private fun finish() {
        if (tmpLengths.position() > 0) {
            tmpLengths.flip()
            tmpStarts.flip()
            tmpStartsI.flip()
            if (umrIndexData != null) {
                glMultiDrawElements(drawMode.id, tmpLengths, INDEX_TYPE, tmpStartsI)
            } else {
                glMultiDrawArrays(drawMode.id, tmpStarts, tmpLengths)
            }
            tmpStarts.clear()
            tmpStartsI.clear()
            tmpLengths.clear()
        }
    }

    override fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean) {
        if (totalNumPrimitives == 0L) return
        val buffer = umrVertexData.buffer
        if (!buffer.isUpToDate) {
            LOGGER.warn("Buffer ${hash32(buffer)} isn't ready")
            return
        }
        GFXState.bind()
        // doesn't matter as long as it's greater than zero; make it the actual value for debugging using DebugGPUStorage
        buffer.drawLength = max(min(totalNumPrimitives, Int.MAX_VALUE.toLong()).toInt(), 1)
        bindBuffer(shader, buffer)

        if (umrIndexData != null) {
            val buffer = umrIndexData.buffer
            buffer.ensureBuffer()
            GPUBuffer.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer.pointer)
        }

        val factor = max(verticesPerEntry, 1)
        val frustum = pipeline?.frustum
        var transform = transform?.globalTransform
        // small optimization: most UniqueMeshRenderers will be at the origin
        if (transform != null && transform.isIdentity()) transform = null
        val self = this
        val iterator = object : UMRIterator<Mesh> {
            override fun filter(entry: Mesh): Boolean = shallRenderEntry(frustum, transform, entry)
            override fun getRange(entry: Mesh): IntRange =
                if (umrIndexData != null) getIndexRange(entry)
                else getVertexRange(entry)

            override fun push(start: Int, endExcl: Int) {
                self.push(start * factor, endExcl * factor)
            }
        }
        val iterData = umrIndexData ?: umrVertexData
        val counter = iterator.iterateRanges(iterData.sortedEntries) * factor
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

    override fun destroy() {
        umrVertexData.destroy()
    }

    open fun clear(destroyMeshes: Boolean) {
        umrVertexData.clear(destroyMeshes)
        numPrimitives = 0
    }

    companion object {
        private val LOGGER = LogManager.getLogger(UniqueMeshRenderer::class)
        private const val INDEX_TYPE = GL_UNSIGNED_INT

        private const val DRAW_CAPACITY = 1024
        private fun createBuffer(): IntBuffer {
            return ByteBufferPool.allocateDirect(4 * DRAW_CAPACITY).asIntBuffer()
        }

        private val tmpStarts = createBuffer()
        private val tmpLengths = createBuffer()
        private val tmpStartsI = PointerBuffer.allocateDirect(DRAW_CAPACITY)
    }
}