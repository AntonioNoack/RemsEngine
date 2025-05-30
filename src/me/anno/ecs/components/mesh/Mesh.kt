package me.anno.ecs.components.mesh

import me.anno.cache.ICacheData
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Order
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.HelperMesh.Companion.destroyHelperMeshes
import me.anno.ecs.components.mesh.MeshAttribute.Companion.copyOf
import me.anno.ecs.components.mesh.MeshAttributes.boneIndicesType
import me.anno.ecs.components.mesh.MeshAttributes.boneWeightsType
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.MeshAttributes.coordsType
import me.anno.ecs.components.mesh.MeshAttributes.normalsType
import me.anno.ecs.components.mesh.MeshAttributes.tangentsType
import me.anno.ecs.components.mesh.MeshAttributes.uvsType
import me.anno.ecs.components.mesh.MeshBufferUtils.createMeshBufferImpl
import me.anno.ecs.components.mesh.MeshIterators.forEachPoint
import me.anno.ecs.components.mesh.TransformMesh.rotateX90DegreesImpl
import me.anno.ecs.components.mesh.TransformMesh.scale
import me.anno.ecs.components.mesh.utils.IndexGenerator.generateIndices
import me.anno.ecs.components.mesh.utils.MorphTarget
import me.anno.ecs.components.mesh.utils.NormalCalculator
import me.anno.ecs.components.mesh.utils.TangentCalculator
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.CullMode
import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
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
import me.anno.mesh.MeshRendering.drawImpl
import me.anno.mesh.MeshRendering.drawInstancedImpl
import me.anno.mesh.MeshUtils.countPrimitives
import me.anno.utils.InternalAPI
import me.anno.utils.algorithms.ForLoop.forLoop
import me.anno.utils.structures.lists.Lists.firstOrNull2
import me.anno.utils.types.Arrays.resize
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

// open, so you can define your own properties;
// custom attributes can be added to vertexAttributes
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
    var vertexAttributes = ArrayList<MeshAttribute>()

    @Type("List<MorphTarget>")
    var morphTargets: List<MorphTarget> = emptyList()

    @Docs("Maps bone indices to names & hierarchy")
    @Type("Skeleton/Reference")
    override var skeleton: FileReference = InvalidRef

    @Docs("Position in local space, packed (x,y,z)")
    @NotSerializedProperty
    var positions: FloatArray? // todo change "positions" to "positions" everywhere
        get() = getAttr("positions", FloatArray::class)
        set(value) = setAttr("positions", value, coordsType)

    @Docs("Normals in local space, packed (nx,ny,nz), will be generated automatically if missing")
    @Type("FloatArray?")
    @NotSerializedProperty
    var normals: FloatArray?
        get() = getAttr("normals", FloatArray::class)
        set(value) = setAttr("normals", value, normalsType)

    @Docs("Tangents in local space, packed (tx,ty,tz,tw)")
    @Type("FloatArray?")
    @NotSerializedProperty
    var tangents: FloatArray?
        get() = getAttr("tangents", FloatArray::class)
        set(value) = setAttr("tangents", value, tangentsType)

    @Docs("Texture coordinates, packed (u,v)")
    @Type("FloatArray?")
    @NotSerializedProperty
    var uvs: FloatArray?
        get() = getAttr("uvs", FloatArray::class)
        set(value) = setAttr("uvs", value, uvsType)

    @Type("FloatArray?")
    @HideInInspector
    var boneWeights: FloatArray?
        get() = getAttr("boneWeights", FloatArray::class)
        set(value) = setAttr("boneWeights", value, boneWeightsType)

    @Type("ByteArray?")
    @HideInInspector
    var boneIndices: ByteArray?
        get() = getAttr("boneIndices", ByteArray::class)
        set(value) = setAttr("boneIndices", value, boneIndicesType)

    @DebugProperty
    val hasBones get() = boneIndices?.isEmpty() == false

    @DebugProperty
    val isIndexed get() = indices != null

    /**
     * maps (triangle/line-)indices to vertices
     * not a traditional MeshAttribute
     * */
    @HideInInspector
    var indices: IntArray? = null

    @SerializedProperty
    @Type("List<Material/Reference>")
    override var materials: List<FileReference> = defaultMaterials

    /**
     * one index per triangle;
     * not a traditional MeshAttribute
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

    fun unlinkGPUData() {
        buffer = null
        triBuffer = null
        needsMeshUpdate = true
    }

    fun unlinkGeometry() {
        val attributes = vertexAttributes
        for (i in attributes.indices) {
            val attribute = attributes[i]
            attribute.data = copyOf(attribute.data)
        }
        indices = indices?.copyOf()
        // how about material IDs?
    }

    fun shallowClone(): Mesh {
        val clone = clone() as Mesh
        clone.unlinkPrefab()
        return clone
    }

    fun deepClone(): Mesh {
        val clone = shallowClone()
        clone.unlinkGPUData()
        clone.unlinkGeometry()
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Mesh) return
        // ensureBuffer()
        // materials
        dst.materials = materials
        // mesh data
        dst.vertexAttributes = ArrayList(vertexAttributes)
        for (i in dst.vertexAttributes.indices) {
            dst.vertexAttributes[i] = dst.vertexAttributes[i].shallowCopy()
        }
        dst.indices = indices
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
        dst.hasUVs = hasUVs
        dst.hasVertexColors = hasVertexColors
        dst.hasBonesInBuffer = hasBonesInBuffer
        dst.helperMeshes = helperMeshes
        dst.hasHighPrecisionNormals = hasHighPrecisionNormals
        dst.skeleton = skeleton
        // aabb
        dst.aabb.set(aabb)
        dst.ignoreStrayPointsInAABB = ignoreStrayPointsInAABB
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            // support for legacy files
            "positions" -> positions = value as? FloatArray ?: return
            "normals" -> normals = value as? FloatArray ?: return
            "uvs" -> uvs = value as? FloatArray ?: return
            "color0" -> color0 = value as? IntArray ?: return
            "boneWeights" -> boneWeights = value as? FloatArray ?: return
            "boneIndices" -> {
                boneIndices = if (value is IntArray) {
                    ByteArray(value.size) { value[it].toByte() }
                } else {
                    value as? ByteArray ?: return
                }
            }
            else -> {
                if (!setSerializableProperty(name, value)) {
                    super.setProperty(name, value)
                }
            }
        }
    }

    override val approxSize get() = 300

    fun calculateAABB() {
        aabb.clear()
        forEachPoint(ignoreStrayPointsInAABB) { x, y, z ->
            aabb.union(x, y, z)
            false
        }
    }

    fun calculateNormals(smooth: Boolean) {
        if (smooth && indices == null) generateIndices()
        val positions = positions ?: return
        val normals = normals.resize(positions.size)
        normals.fill(0f)
        NormalCalculator.checkNormals(this, positions, normals, indices, drawMode)
        this.normals = normals
    }

    @NotSerializedProperty
    var buffer: StaticBuffer? = null

    @NotSerializedProperty
    var triBuffer: IndexBuffer? = null

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

    override fun destroy() {
        // todo only if we were not cloned...
        destroyHelperMeshes()
        clearGPUData()
        // clearCPUData()
    }

    fun clearGPUData() {
        buffer?.destroy()
        triBuffer?.destroy()
        buffer = null
        triBuffer = null
        needsMeshUpdate = true
    }

    fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int) {
        draw(pipeline, shader, materialIndex, drawDebugLines)
    }

    override fun draw(pipeline: Pipeline?, shader: Shader, materialIndex: Int, drawLines: Boolean) {
        val proceduralLength = proceduralLength
        if (proceduralLength <= 0) {
            drawImpl(shader, materialIndex)
        } else if ((positions?.size ?: 0) == 0) {
            StaticBuffer.drawArraysNull(shader, drawMode, proceduralLength)
        } else {
            (triBuffer ?: buffer)?.drawInstanced(shader, proceduralLength)
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
            false
        }
        return aabb
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        pipeline.addMesh(this, Pipeline.sampleMeshComponent, transform)
    }

    @DebugAction("Center XZ, on Floor")
    fun centerXZonY() {
        val bounds = getBounds()
        val dx = -bounds.centerX
        val dy = -bounds.minY
        val dz = -bounds.centerZ
        move(dx, dy, dz)
    }

    @DebugAction("Center XYZ")
    fun centerXYZ() {
        val bounds = getBounds()
        val dx = -bounds.centerX
        val dy = -bounds.centerY
        val dz = -bounds.centerZ
        move(dx, dy, dz)
    }

    @DebugAction
    @Order(1000)
    fun move(dx: Float, dy: Float, dz: Float) {
        if (prefab?.isWritable == false) {
            warnIsImmutable()
        } else {
            val positions = positions ?: return
            forLoop(0, positions.size, 3) { i ->
                positions[i] += dx
                positions[i + 1] += dy
                positions[i + 2] += dz
            }
            invalidateGeometry()
        }
    }

    @DebugAction("Scale Up 10x")
    fun scaleUp10x() {
        scale(Vector3f(10f))
    }

    @DebugAction("Scale Down 10x")
    fun scaleDown10x() {
        scale(Vector3f(0.1f))
    }

    @DebugAction("Rotate X90°/SwapYZ")
    fun rotateX90Degrees() {
        rotateX90DegreesImpl()
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

    private fun getAttribute(name: String): MeshAttribute? {
        return vertexAttributes.firstOrNull2 { it.attribute.name == name }
    }

    fun <V : Any> getAttr(name: String, clazz: KClass<V>): V? {
        val value = getAttribute(name)?.data
        return clazz.safeCast(value)
    }

    fun <V : Any> setAttr(name: String, value: V?, attrType: Attribute) {
        val attributes = vertexAttributes
        if (value == null) {
            attributes.removeIf { it.attribute.name == name }
        } else {
            var attribute = attributes.firstOrNull2 { it.attribute.name == name }
            if (attribute == null) {
                attribute = MeshAttribute(attrType.withName(name), value)
                attributes.add(attribute)
            } else {
                attribute.data = value
            }
        }
    }

    companion object {

        fun drawDebugLines(mode: RenderMode?): Boolean {
            return mode != null && mode.renderLines
        }

        val drawDebugLines: Boolean
            get() = drawDebugLines(RenderView.currentInstance?.renderMode)

        private val defaultMaterials = emptyList<FileReference>()
        private val LOGGER = LogManager.getLogger(Mesh::class)

        // custom attributes for shaders? idk...
        // will always be 4, so bone indices can be aligned
        const val MAX_WEIGHTS = 4
    }
}