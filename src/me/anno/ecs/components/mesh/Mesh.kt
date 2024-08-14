package me.anno.ecs.components.mesh

import me.anno.cache.ICacheData
import me.anno.ecs.Transform
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.HelperMesh.Companion.destroyHelperMeshes
import me.anno.ecs.components.mesh.HelperMesh.Companion.updateHelperMeshes
import me.anno.ecs.components.mesh.MeshBufferUtils.replaceBuffer
import me.anno.ecs.components.mesh.MeshBufferUtils.updateMesh
import me.anno.ecs.components.mesh.MeshIterators.countLines
import me.anno.ecs.components.mesh.MeshIterators.forEachLineIndex
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
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
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
import me.anno.utils.InternalAPI
import me.anno.utils.structures.lists.Lists.arrayListOfNulls
import me.anno.utils.structures.lists.Lists.wrap
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.roundToIntOr
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.min

// open, so you can define your own attributes
open class Mesh : PrefabSaveable(), IMesh, Renderable, ICacheData {

    @NotSerializedProperty
    var raycaster: BLASNode? = null

    // use buffers instead, so they can be uploaded directly? no, I like the strided thing...
    // but it may be more flexible... still in Java, FloatArrays are more comfortable
    // -> just use multiple setters for convenience

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

    override fun clone(): PrefabSaveable {
        val clone = Mesh()
        copyInto(clone)
        return clone
    }

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
        dst as Mesh
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
        val indices = indices
        // if the indices array is empty, it indicates a non-indexed array, so all values will be considered
        val positions = positions ?: return
        if (ignoreStrayPointsInAABB && indices != null && indices.isNotEmpty()) {
            for (i in indices.indices) {
                val i3 = indices[i] * 3
                val x = positions[i3]
                val y = positions[i3 + 1]
                val z = positions[i3 + 2]
                aabb.union(x, y, z)
            }
        } else {
            for (index in 0 until positions.size - 2 step 3) {
                val x = positions[index]
                val y = positions[index + 1]
                val z = positions[index + 2]
                aabb.union(x, y, z)
            }
        }
        // LOGGER.info("Collected aabb $aabb from ${positions?.size}/${indices?.size} points")
    }

    @Suppress("unused")
    fun calculateNormals(smooth: Boolean) {
        val positions = positions!!
        if (smooth && indices == null) {
            indices = NormalCalculator.generateIndices(positions, uvs, color0, materialIds, boneIndices, boneWeights)
            LOGGER.debug("Generated indices for mesh")
        }
        normals = FloatArray(positions.size)
        NormalCalculator.checkNormals(this, positions, normals!!, indices, drawMode)
    }

    /**
     * throws an IllegalStateException, if anything is incorrectly set up
     * if this succeeds, then the drawing routine should not crash
     * */
    fun checkCompleteness() {
        if (proceduralLength > 0) return
        // check whether all variables are set correctly
        val positions = positions
        val normals = normals
        val uvs = uvs
        if (positions == null) throw IllegalStateException("Missing positions, normals? ${normals?.size}, uvs? ${uvs?.size}")
        if (positions.size % 3 != 0) throw IllegalStateException("Positions must be a vector of vec3, but ${positions.size} % 3 != 0, it's ${positions.size % 3}")
        // while incorrect, the following should not cause an exception
        /*if (normals != null && normals.size != positions.size) throw IllegalStateException("Size of normals doesn't match size of positions")
        if (uvs != null) {
            if (uvs.size * 3 != positions.size * 2) throw IllegalStateException("Size of UVs does not match size of positions: ${positions.size}*2 vs ${uvs.size}*3")
        }
        val boneWeights = boneWeights
        val boneIndices = boneIndices
        if ((boneIndices == null) != (boneWeights == null)) throw IllegalStateException("Needs both or neither bone weights and indices")
        if (boneWeights != null && boneIndices != null) {
            if (boneWeights.size != boneIndices.size)
                throw IllegalStateException("Size of bone weights must match size of bone indices, ${boneWeights.size} vs ${boneIndices.size}")
            if (boneWeights.size * 3 != positions.size * MAX_WEIGHTS)
                throw IllegalStateException(
                    "Size of weights does not match positions, there must be $MAX_WEIGHTS weights per vertex, " +
                            "${boneWeights.size} * 3 vs ${positions.size} * $MAX_WEIGHTS"
                )
        }
        val color0 = color0
        if (color0 != null && color0.size * 3 != positions.size) throw IllegalStateException("Every vertex needs an ARGB color value")*/
        val indices = indices
        if (indices != null) {
            // check all indices for correctness
            val vertexCount = positions.size / 3
            for (i in indices) {
                if (i !in 0 until vertexCount) {
                    throw IllegalStateException("Vertex Index is out of bounds: $i !in 0 until $vertexCount")
                }
            }
        }
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
        get(): Long {
            val indices = indices
            val positions = positions
            val drawMode = drawMode
            val baseLength = if (indices != null) {
                numPrimitivesByType(indices.size * 3, drawMode)
            } else if (positions != null) {
                numPrimitivesByType(positions.size, drawMode)
            } else 0
            val size = proceduralLength
            return if (size <= 0) baseLength.toLong()
            else if (baseLength > 0) baseLength.toLong() * size
            else numPrimitivesByType(size, drawMode).toLong()
        }

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
            if (needsMeshUpdate) updateMesh()
            if (GFX.isGFXThread()) buffer?.ensureBuffer()
        }
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
            ensureBuffer()
            // respect the material index: only draw what belongs to the material
            val helperMeshes = helperMeshes
            when {
                helperMeshes != null && materialIndex in helperMeshes.indices -> {
                    val helperMesh = helperMeshes[materialIndex] ?: return
                    if (drawLines) {
                        helperMesh.ensureDebugLines(this)
                        helperMesh.debugLineBuffer?.draw(shader)
                    } else {
                        helperMesh.triBuffer?.draw(shader)
                        helperMesh.lineBuffer?.draw(shader)
                    }
                }
                materialIndex == 0 -> {
                    if (drawLines) {
                        ensureDebugLines()
                        debugLineBuffer?.draw(shader)
                    } else {
                        (triBuffer ?: buffer)?.draw(shader)
                        lineBuffer?.draw(shader)
                    }
                }
            }
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
            GFX.check()
            ensureBuffer()
            GFX.check()
            // respect the material index: only draw what belongs to the material
            val helperMeshes = helperMeshes
            if (helperMeshes != null) {
                val helperMesh = helperMeshes.getOrNull(materialIndex)
                if (helperMesh != null) {
                    if (drawDebugLines) {
                        helperMesh.ensureDebugLines(this)
                        helperMesh.debugLineBuffer?.drawInstanced(shader, instanceData)
                    } else {
                        helperMesh.triBuffer?.drawInstanced(shader, instanceData)
                        helperMesh.lineBuffer?.drawInstanced(shader, instanceData)
                    }
                }
            } else if (materialIndex == 0) {
                if (drawDebugLines) {
                    ensureDebugLines()
                    debugLineBuffer?.drawInstanced(shader, instanceData)
                } else {
                    (triBuffer ?: buffer)?.drawInstanced(shader, instanceData)
                    lineBuffer?.drawInstanced(shader, instanceData)
                }
            }
            GFX.check()
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
        getBounds()
        forEachPoint(onlyFaces) { x, y, z ->
            aabb.union(transform.transformProject(vf.set(x, y, z)))
        }
        return aabb
    }

    fun makeFlatShaded(calculateNormals: Boolean = true) {
        val indices = indices
        val positions = positions ?: return
        val colors = color0
        if (indices == null) {
            if (calculateNormals) calculateNormals(false)
        } else {
            val newPositions = FloatArray(indices.size * 3)
            val newColors = if (colors != null) IntArray(indices.size) else null
            for (i in indices.indices) {
                val i3 = i * 3
                val j = indices[i]
                val j3 = j * 3
                newPositions[i3] = positions[j3]
                newPositions[i3 + 1] = positions[j3 + 1]
                newPositions[i3 + 2] = positions[j3 + 2]
                if (colors != null) newColors!![i] = colors[j]
            }
            this.positions = newPositions
            this.normals = normals.resize(newPositions.size)
            this.color0 = newColors
            this.indices = null
            if (calculateNormals) calculateNormals(false)
        }
    }

    fun makeLineMesh(keepOnlyUniqueLines: Boolean) {
        val indices = if (keepOnlyUniqueLines) {
            val lines = HashSet<IntPair>()
            forEachLineIndex { a, b ->
                if (a != b) {
                    lines += IntPair(min(a, b), max(a, b))
                }
            }
            var ctr = 0
            val indices = indices.resize(lines.size * 2)
            for (line in lines) {
                indices[ctr++] = line.first
                indices[ctr++] = line.second
            }
            indices
        } else {
            var ctr = 0
            val indices = indices.resize(countLines() * 2)
            forEachLineIndex { a, b ->
                indices[ctr++] = a
                indices[ctr++] = b
            }
            indices
        }
        this.indices = indices
        drawMode = DrawMode.LINES
        invalidateGeometry()
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