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
import me.anno.utils.Clock
import me.anno.utils.Logging.hash32
import me.anno.utils.assertions.assertTrue
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.size
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix4x3
import org.lwjgl.opengl.GL46C.glMultiDrawArrays
import java.nio.IntBuffer

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
) : MeshSpawner(), IMesh, ICacheData, AllocationManager<MeshEntry<Mesh>, StaticBuffer> {

    /**
     * Implement this method to pack your custom mesh type into a StaticBuffer to be copied around.
     * Returning null will ignore your element.
     * */
    abstract fun createBuffer(key: Key, mesh: Mesh): StaticBuffer?

    /**
     * Return transform and material for that specific mesh for traditional methods.
     * Rendering must handle transform and material without this function:
     * - transform must be encoded in the buffer somehow
     * - material is taken from UniqueMeshRender.cachedMaterials
     * */
    open fun getTransformAndMaterial(key: Key, transform: Transform): Material? = null

    /**
     * defines what the world looks like for Raycasting,
     * and for AABBs
     * */
    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {
        var i = 0
        for ((key, entry) in entries) {
            val transform = getTransform(i++)
            val material = getTransformAndMaterial(key, transform)
            val mesh = entry.mesh as? IMesh ?: continue
            if (callback(mesh, material, transform)) break
        }
    }

    private val entries = HashMap<Key, MeshEntry<Mesh>>()
    private val sortedEntries = ArrayList<MeshEntry<Mesh>>()
    private val sortedRanges = ArrayList<IntRange>()

    private var buffer0 = StaticBuffer("umr0", attributes, 0, BufferUsage.DYNAMIC)
    private var buffer1 = StaticBuffer("umr1", attributes, 0, BufferUsage.DYNAMIC)

    val stride get() = attributes.stride
    val values: List<MeshEntry<Mesh>> get() = sortedEntries

    @DebugProperty
    @NotSerializedProperty
    override var numPrimitives: Long = 0

    var totalNumPrimitives = 0L

    override fun ensureBuffer() {
        // not really anything to do for now...
    }

    private val boundsF = AABBf()
    override fun getBounds(): AABBf = boundsF

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        // calculate local aabb
        val local = boundsF
        local.clear()
        for (i in sortedEntries.indices) {
            val entry = sortedEntries[i]
            local.union(entry.localBounds)
        }
        localAABB.set(local)

        // calculate global aabb
        val global = globalAABB
        local.transform(globalTransform, global)

        // add the result to the output
        dstUnion.union(global)
    }

    val clock = Clock(LOGGER)

    operator fun set(key: Key, entry: MeshEntry<Mesh>): Boolean {
        val old = entries[key]
        if (old != null) remove(key, entry.mesh !== old.mesh)
        return add(key, entry)
    }

    operator fun get(key: Key): MeshEntry<Mesh>? = entries[key]

    fun add(key: Key, entry: MeshEntry<Mesh>): Boolean {
        if (key in entries) return false
        val b0 = buffer0
        val b1 = buffer1
        clock.start()
        val bx = insert(
            sortedEntries, sortedRanges, entry, entry.buffer,
            entry.range, b0.vertexCount, b0,
            false
        ).second
        clock.stop("Insert", 0.01)
        entries[key] = entry
        totalNumPrimitives += entry.buffer.vertexCount
        assertTrue(bx === b0 || bx === b1)
        this.buffer1 = if (bx === b1) b0 else b1
        this.buffer0 = if (bx === b1) b1 else b0
        invalidateBounds()
        return true
    }

    fun add(key: Key, mesh: Mesh, bounds: AABBd): Boolean {
        if (key in entries) return false
        val buffer = createBuffer(key, mesh) ?: return false
        val entry = MeshEntry(mesh, bounds, buffer)
        return add(key, entry)
    }

    fun remove(key: Key, destroyMesh: Boolean): Mesh? {
        val entry = this@UniqueMeshRenderer.entries.remove(key) ?: return null
        assertTrue(remove(entry, sortedEntries, sortedRanges))
        totalNumPrimitives -= entry.buffer.vertexCount
        if (destroyMesh && entry.mesh is ICacheData) {
            entry.mesh.destroy()
        }
        entry.buffer.destroy()
        invalidate()
        return entry.mesh
    }

    var isValid = true

    fun invalidate() {
        isValid = false
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        clickId = pipeline.getClickId(this)
        pipeline.addMesh(this, this, transform)
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
        if (totalNumPrimitives == 0L) return
        val buffer = buffer0
        if (!buffer.isUpToDate) {
            LOGGER.warn("Buffer ${hash32(buffer)} isn't ready")
            return
        }
        var counter = 0L
        GFXState.bind()
        buffer.drawLength = 1 // doesn't matter as long as it's greater than zero
        buffer.bind(shader)
        val frustum = pipeline?.frustum
        var transform = transform?.globalTransform
        // small optimization: most UniqueMeshRenderers will be at the origin
        if (transform != null && transform.isIdentity()) transform = null
        var currStart = 0
        var currEnd = 0
        for (i in sortedEntries.indices) {
            val entry = sortedEntries[i]
            if (shallRenderEntry(frustum, transform, entry)) {
                val range = entry.range
                if (range.first != currEnd) {
                    push(buffer, currStart, currEnd)
                    counter += currEnd - currStart
                    currStart = range.first
                }
                currEnd = range.last + 1
            }
        }
        push(buffer, currStart, currEnd)
        counter += currEnd - currStart
        numPrimitives = counter
        finish(buffer)
        buffer.unbind()
    }

    open fun shallRenderEntry(frustum: Frustum?, transform: Matrix4x3?, entry: MeshEntry<Mesh>): Boolean {
        return if (frustum != null) {
            val globalBounds = if (transform != null) {
                val tmpBounds = JomlPools.aabbd.borrow()
                entry.localBounds.transform(transform, tmpBounds)
            } else entry.localBounds
            // frustum culling
            frustum.isVisible(globalBounds)
        } else true
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
        val oldSize = buffer.vertexCount
        buffer.vertexCount = newSize
        if (newSize > 0) {
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
        for ((_, entry) in entries) {
            entry.buffer.destroy()
            if (entry.mesh is ICacheData) entry.mesh.destroy()
        }
    }

    open fun clear(destroyMeshes: Boolean) {
        entries.clear()
        for (entry in sortedEntries) {
            if (destroyMeshes && entry.mesh is ICacheData) entry.mesh.destroy()
            entry.buffer.destroy()
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