package me.anno.ecs.components.mesh

import me.anno.cache.ICacheData
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.HelperMesh.Companion.destroyHelperMeshes
import me.anno.ecs.components.mesh.MeshBufferUtils.createMeshBufferImpl
import me.anno.ecs.components.mesh.MeshBufferUtils.replaceBuffer
import me.anno.ecs.components.mesh.MeshIterators.forEachPoint
import me.anno.ecs.components.mesh.utils.MorphTarget
import me.anno.ecs.components.mesh.utils.NormalCalculator
import me.anno.ecs.components.mesh.utils.TangentCalculator
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.CullMode
import me.anno.gpu.GFX
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.IndexBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.temporary.InnerTmpPrefabFile
import me.anno.maths.bvh.BLASNode
import me.anno.mesh.FindLines
import me.anno.mesh.MeshRendering.drawImpl
import me.anno.mesh.MeshRendering.drawInstancedImpl
import me.anno.mesh.MeshUtils.countPrimitives
import me.anno.utils.InternalAPI
import me.anno.utils.structures.lists.Lists.wrap
import me.anno.utils.types.Arrays.resize
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.max

// open, so you can define your own attributes
open class Mesh : PrefabSaveable(), IMesh, Renderable, ICacheData {

    @NotSerializedProperty
    var raycaster: BLASNode? = null

    @InternalAPI
    @NotSerializedProperty
    var needsMeshUpdate = true

    @NotSerializedProperty
    private var needsBoundsUpdate = true

    override var cullMode = CullMode.FRONT

    /**
     * call this function, when you have changed the geometry;
     * on the next frame, the new mesh data will be uploaded to the GPU
     * */
    @DebugAction
    fun invalidateGeometry() {
        needsMeshUpdate = true
        needsBoundsUpdate = true
        raycaster = null
        val ref = refOrNull
        if (ref is InnerTmpPrefabFile) {
            ref.markAsModified()
        }
    }

    // todo also we need a renderer, which can handle morphing
    // todo or we need to compute it on the cpu

    override var proceduralLength = 0

    var inverseOutline = false

    @HideInInspector
    var positions: FloatArray? = null

    @Type("List<MorphTarget>")
    var morphTargets: List<MorphTarget> = emptyList()

    @Docs("Normals in local space, will be generated automatically if missing")
    @Type("FloatArray?")
    @HideInInspector
    var normals: FloatArray? = null

    @Type("FloatArray?")
    @HideInInspector
    var tangents: FloatArray? = null

    @Type("FloatArray?")
    @HideInInspector
    var uvs: FloatArray? = null

    // colors, rgba,
    // the default shader only supports the first color
    // other colors still can be loaded for ... idk... maybe terrain information or sth like that
    @Type("IntArray?")
    @HideInInspector
    var color0: IntArray? = null

    @Type("IntArray?")
    @HideInInspector
    var color1: IntArray? = null

    @Type("IntArray?")
    @HideInInspector
    var color2: IntArray? = null

    @Type("IntArray?")
    @HideInInspector
    var color3: IntArray? = null

    @Type("IntArray?")
    @HideInInspector
    var color4: IntArray? = null

    @Type("IntArray?")
    @HideInInspector
    var color5: IntArray? = null

    @Type("IntArray?")
    @HideInInspector
    var color6: IntArray? = null

    @Type("IntArray?")
    @HideInInspector
    var color7: IntArray? = null

    @Type("FloatArray?")
    @HideInInspector
    var boneWeights: FloatArray? = null

    @Type("ByteArray?")
    @HideInInspector
    var boneIndices: ByteArray? = null

    @DebugProperty
    val hasBones get() = boneIndices?.isEmpty() == false

    @DebugProperty
    val isIndexed get() = indices != null

    @HideInInspector
    @NotSerializedProperty
    var debugLineIndices: IntArray? = null

    @Docs("Automatically found lines (for rendering); can be set manually")
    @NotSerializedProperty
    var lineIndices: IntArray? = null

    @HideInInspector
    var indices: IntArray? = null

    @SerializedProperty
    @Type("List<Material/Reference>")
    override var materials: List<FileReference> = defaultMaterials

    @HideInInspector
    @NotSerializedProperty
    var material: FileReference
        get() = materials.firstOrNull() ?: InvalidRef
        set(value) {
            materials = value.wrap()
        }

    /**
     * one index per triangle
     * */
    @Type("IntArray?")
    @HideInInspector
    var materialIds: IntArray? = null

    override var numMaterials = 1

    @NotSerializedProperty
    var helperMeshes: List<HelperMesh?>? = null

    // to allow for quads, and strips and such
    /**
     * how the positions / indices are drawn;
     * implementations might only support GL_TRIANGLES, so be careful, and always prefer GL_TRIANGLES!
     * */
    var drawMode = DrawMode.TRIANGLES

    @DebugProperty // todo make this assignable to get-functions
    private val debugBounds get() = getBounds()
    private val aabb = AABBf()

    var ignoreStrayPointsInAABB = false

    fun unlink() {
        buffer = null
        triBuffer = null
        lineBuffer = null
        debugLineBuffer = null
        prefabPath = Path.ROOT_PATH
        prefab = null
        needsMeshUpdate = true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Mesh) return
        // ensureBuffer()
        // materials
        dst.materials = materials
        // mesh data
        dst.positions = positions
        dst.normals = normals
        dst.uvs = uvs
        dst.color0 = color0
        dst.color1 = color1
        dst.color2 = color2
        dst.color3 = color3
        dst.color4 = color4
        dst.color5 = color5
        dst.color6 = color6
        dst.color7 = color7
        dst.tangents = tangents
        dst.indices = indices
        dst.lineIndices = lineIndices
        dst.boneWeights = boneWeights
        dst.boneIndices = boneIndices
        dst.materialIds = materialIds
        // morph targets
        dst.morphTargets = morphTargets
        // draw mode
        dst.drawMode = drawMode
        dst.cullMode = cullMode
        // buffer
        dst.needsMeshUpdate = needsMeshUpdate
        dst.buffer = buffer
        dst.triBuffer = triBuffer
        dst.lineBuffer = lineBuffer
        dst.debugLineBuffer = debugLineBuffer
        dst.hasUVs = hasUVs
        dst.hasVertexColors = hasVertexColors
        dst.hasBonesInBuffer = hasBonesInBuffer
        dst.helperMeshes = helperMeshes
        dst.hasHighPrecisionNormals = hasHighPrecisionNormals
        // aabb
        dst.aabb.set(aabb)
        dst.ignoreStrayPointsInAABB = ignoreStrayPointsInAABB
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun setProperty(name: String, value: Any?) {
        if (name == "boneIndices" && value is IntArray) {
            boneIndices = ByteArray(value.size) { value[it].toByte() }
        } else if (!setSerializableProperty(name, value)) {
            super.setProperty(name, value)
        }
    }

    override val approxSize get() = 1

    fun calculateAABB() {
        aabb.clear()
        forEachPoint(ignoreStrayPointsInAABB) { x, y, z ->
            aabb.union(x, y, z)
        }
    }

    fun calculateNormals(smooth: Boolean) {
        val positions = positions!!
        if (smooth && indices == null) {
            indices = NormalCalculator.generateIndices(positions, uvs, color0, materialIds, boneIndices, boneWeights)
            LOGGER.debug("Generated indices for mesh")
        }
        normals = normals.resize(positions.size)
        NormalCalculator.checkNormals(this, positions, normals!!, indices, drawMode)
    }

    @NotSerializedProperty
    var buffer: StaticBuffer? = null

    @NotSerializedProperty
    var triBuffer: IndexBuffer? = null

    @NotSerializedProperty
    var lineBuffer: IndexBuffer? = null

    @NotSerializedProperty
    var debugLineBuffer: IndexBuffer? = null

    @InternalAPI
    @NotSerializedProperty
    var invalidDebugLines = true

    var hasUVs = false
    override var hasVertexColors = 0
    override var hasBonesInBuffer = false

    @DebugProperty
    override val numPrimitives
        get(): Long = countPrimitives()

    fun numPrimitivesByType(numPositionValues: Int, drawMode: DrawMode): Int {
        return when (drawMode) {
            DrawMode.POINTS -> numPositionValues / 3
            DrawMode.LINES -> numPositionValues / 6
            DrawMode.LINE_STRIP -> max(0, numPositionValues / 3 - 1)
            DrawMode.TRIANGLE_STRIP -> max(0, numPositionValues / 3 - 2)
            else -> numPositionValues / 9
        }
    }

    @DebugProperty
    val hasBuffers
        get() = buffer?.run { pointer != 0 } == true

    var hasHighPrecisionNormals = false

    /** can be set false to use tangents as an additional data channel; notice the RGB[-1,1] limit though */
    var checkTangents = true

    override fun getBounds(): AABBf {
        if (needsBoundsUpdate) {
            if (positions != null) {
                needsBoundsUpdate = false
                calculateAABB()
            } else {
                aabb.all()
            }
        }
        return aabb
    }

    /**
     * upload the data to the gpu, if it has changed
     * */
    @DebugAction
    override fun ensureBuffer() {
        synchronized(this) {
            if (needsMeshUpdate) createMeshBuffer()
            if (GFX.isGFXThread()) buffer?.ensureBuffer()
        }
    }

    open fun createMeshBuffer() {
        createMeshBufferImpl()
    }

    fun ensureNorTanUVs() {

        // not the safest, but well...
        val positions = positions ?: return

        // if normals are null or have length 0, compute them
        if (normals == null)
            normals = FloatArray(positions.size)

        if (tangents == null && uvs != null) // tangents are only computable, if we have uvs
            tangents = FloatArray(positions.size / 3 * 4)

        val normals = normals!!
        val tangents = tangents

        val uvs = uvs
        val hasUVs = uvs != null && uvs.isNotEmpty()
        this.hasUVs = hasUVs

        NormalCalculator.checkNormals(this, positions, normals, indices, drawMode)
        if (hasUVs && checkTangents)
            TangentCalculator.checkTangents(this, positions, normals, tangents, uvs)
    }

    fun ensureDebugLines() {
        val buffer = buffer
        if (invalidDebugLines && buffer != null) {
            invalidDebugLines = false
            debugLineIndices = FindLines.getAllLines(this, indices)
            debugLineBuffer = replaceBuffer(buffer, debugLineIndices, debugLineBuffer)
            debugLineBuffer?.drawMode = DrawMode.LINES
        }
    }

    override fun destroy() {
        // todo only if we were not cloned...
        destroyHelperMeshes()
        clearGPUData()
        // clearCPUData()
    }

    fun clearGPUData() {
        buffer?.destroy()
        triBuffer?.destroy()
        lineBuffer?.destroy()
        debugLineBuffer?.destroy()
        buffer = null
        triBuffer = null
        debugLineBuffer = null
        needsMeshUpdate = true
    }

    fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int) {
        draw(pipeline, shader, materialIndex, drawDebugLines)
    }

    override fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean) {
        val proceduralLength = proceduralLength
        if (proceduralLength <= 0) {
            drawImpl(shader, materialIndex, drawLines)
        } else if ((positions?.size ?: 0) == 0) {
            StaticBuffer.drawArraysNull(shader, drawMode, proceduralLength)
        } else {
            if (drawLines) {
                ensureDebugLines()
                debugLineBuffer?.drawInstanced(shader, proceduralLength)
            } else {
                (triBuffer ?: buffer)?.drawInstanced(shader, proceduralLength)
                lineBuffer?.drawInstanced(shader, proceduralLength)
            }
        }
    }

    override fun drawInstanced(
        pipeline: Pipeline, shader: Shader, materialIndex: Int,
        instanceData: Buffer, drawLines: Boolean
    ) {
        if (proceduralLength <= 0) {
            drawInstancedImpl(shader, materialIndex, instanceData)
        } else LOGGER.warn("Instanced rendering of procedural meshes is not supported!")
    }

    /**
     * calculates the bounds of the mesh
     * not fast, but the gpu will take just as long -> doesn't matter
     *
     * the goal is to be accurate
     * */
    fun getBounds(transform: Matrix4f, onlyFaces: Boolean): AABBf {
        val vf = Vector3f()
        val aabb = AABBf()
        forEachPoint(onlyFaces) { x, y, z ->
            aabb.union(transform.transformProject(vf.set(x, y, z)))
        }
        return aabb
    }

    override fun fill(pipeline: Pipeline, transform: Transform, clickId: Int): Int {
        pipeline.addMesh(this, Pipeline.sampleMeshComponent, transform)
        return clickId
    }

    @DebugAction
    fun centerXZonY() {
        val bounds = getBounds()
        val dx = -bounds.centerX
        val dy = -bounds.minY
        val dz = -bounds.centerZ
        move(dx, dy, dz)
    }

    @DebugAction
    fun centerXYZ() {
        val bounds = getBounds()
        val dx = -bounds.centerX
        val dy = -bounds.centerY
        val dz = -bounds.centerZ
        move(dx, dy, dz)
    }

    @DebugAction
    fun move(dx: Float, dy: Float, dz: Float) {
        if (prefab?.isWritable == false) {
            warnIsImmutable()
        } else {
            val positions = positions ?: return
            for (i in 0 until positions.size - 2 step 3) {
                positions[i] += dx
                positions[i + 1] += dy
                positions[i + 2] += dz
            }
            invalidateGeometry()
        }
    }

    @DebugAction
    fun scaleUp100x() {
        scale(Vector3f(100f))
    }

    @DebugAction
    fun scaleDown100x() {
        scale(Vector3f(0.01f))
    }

    fun scale(factor: Vector3f) {
        val positions = positions ?: return
        for (i in positions.indices step 3) {
            positions[i] *= factor.x
            positions[i + 1] *= factor.y
            positions[i + 2] *= factor.z
        }
        invalidateGeometry()
    }

    @DebugAction
    fun removeVertexColors() {
        if (prefab?.isWritable == false) {
            warnIsImmutable()
        } else {
            color0 = null
            prefab?.set("color0", null)
            invalidateGeometry()
        }
    }

    private fun warnIsImmutable() {
        LOGGER.warn("Mesh is immutable")
    }

    companion object {

        val drawDebugLines: Boolean
            get() {
                val mode = RenderView.currentInstance?.renderMode
                return mode == RenderMode.LINES || mode == RenderMode.LINES_MSAA
            }

        private val defaultMaterials = emptyList<FileReference>()
        private val LOGGER = LogManager.getLogger(Mesh::class)

        // custom attributes for shaders? idk...
        // will always be 4, so bone indices can be aligned
        const val MAX_WEIGHTS = 4
    }
}