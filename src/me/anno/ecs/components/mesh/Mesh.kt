package me.anno.ecs.components.mesh

import me.anno.cache.ICacheData
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Type
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.buffer.*
import me.anno.gpu.buffer.Attribute.Companion.computeOffsets
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.FindLines
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Vectors.set2
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C.GL_LINES
import org.lwjgl.opengl.GL11C.GL_TRIANGLES
import kotlin.math.max
import kotlin.math.roundToInt

class Mesh : PrefabSaveable(), Renderable {

    // a single helper mesh could be used to represent the default indices...
    class HelperMesh(val indices: IntArray) : ICacheData {

        var triBuffer: IndexBuffer? = null
        var lineBuffer: IndexBuffer? = null
        var debugLineBuffer: IndexBuffer? = null
        var debugLineIndices: IntArray? = null
        var invalidDebugLines = true
        var lineIndices: IntArray? = null

        fun init(mesh: Mesh) {
            val buffer = mesh.buffer!!
            triBuffer = IndexBuffer(buffer, indices)
            triBuffer?.drawMode = mesh.drawMode

            lineIndices = lineIndices ?: FindLines.findLines(indices, mesh.positions)
            lineBuffer = replaceBuffer(buffer, lineIndices, lineBuffer)
            lineBuffer?.drawMode = GL_LINES
        }

        fun ensureDebugLines(mesh: Mesh) {
            val buffer = mesh.buffer
            if (invalidDebugLines && buffer != null) {
                invalidDebugLines = false
                debugLineIndices = FindLines.getAllLines(indices, mesh.positions, debugLineIndices)
                debugLineBuffer = replaceBuffer(buffer, debugLineIndices, debugLineBuffer)
                debugLineBuffer?.drawMode = GL_LINES
            }
        }

        override fun destroy() {
            triBuffer?.destroy()
            lineBuffer?.destroy()
            debugLineBuffer?.destroy()
        }

    }

    // use buffers instead, so they can be uploaded directly? no, I like the strided thing...
    // but it may be more flexible... still in Java, FloatArrays are more comfortable
    // -> just use multiple setters for convenience

    @NotSerializedProperty
    private var needsMeshUpdate = true

    @NotSerializedProperty
    private var needsBoundsUpdate = true

    /**
     * call this function, when you have changed the geometry;
     * on the next frame, the new mesh data will be uploaded to the GPU
     * */
    fun invalidateGeometry() {
        needsMeshUpdate = true
        needsBoundsUpdate = true
    }

    // todo also we need a renderer, which can handle morphing
    // todo or we need to compute it on the cpu

    /**
     * when this property is > 0, then all vertex data will be ignored;
     * please set positions to a float array (e.g., empty) anyways
     * */
    var proceduralLength = 0

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

    val hasBones get() = boneWeights != null && boneIndices != null

    // todo find lines, and display them
    // triangle (a,b,c), where (a==b||b==c||c==a) && (a!=b||b!=c||c!=a)
    @HideInInspector
    @NotSerializedProperty
    var debugLineIndices: IntArray? = null

    @Docs("Automatically found lines (for rendering); can be set manually")
    @NotSerializedProperty
    var lineIndices: IntArray? = null

    // todo sort them by material/shader, and create multiple buffers (or sub-buffers) for them
    @HideInInspector
    var indices: IntArray? = null

    // allow multiple materials? should make our life easier :), we just need to split the indices...
    // todo filter file references for a specific type for ui
    @SerializedProperty
    @Type("List<Material/Reference>")
    var materials: List<FileReference> = defaultMaterials

    @HideInInspector
    @NotSerializedProperty
    var material: FileReference?
        get() = materials.getOrNull(0)
        set(value) {
            materials = if (value != null) listOf(value)
            else emptyList()
        }

    /**
     * one index per triangle
     * */
    @Type("IntArray?")
    @HideInInspector
    var materialIds: IntArray? = null

    var numMaterials = 1

    @NotSerializedProperty
    var helperMeshes: Array<HelperMesh?>? = null

    // to allow for quads, and strips and such
    var drawMode = GL_TRIANGLES

    val aabb = AABBf()

    var ignoreStrayPointsInAABB = false

    override fun clone(): Mesh {
        // ensureBuffer() // saves buffers
        val clone = Mesh()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Mesh
        // ensureBuffer()
        // materials
        clone.materials = materials
        // mesh data
        clone.positions = positions
        clone.normals = normals
        clone.uvs = uvs
        clone.color0 = color0
        clone.color1 = color1
        clone.color2 = color2
        clone.color3 = color3
        clone.color4 = color4
        clone.color5 = color5
        clone.color6 = color6
        clone.color7 = color7
        clone.tangents = tangents
        clone.indices = indices
        clone.lineIndices = lineIndices
        clone.boneWeights = boneWeights
        clone.boneIndices = boneIndices
        clone.materialIds = materialIds
        // morph targets
        clone.morphTargets = morphTargets
        // draw mode
        clone.drawMode = drawMode
        // buffer
        clone.needsMeshUpdate = needsMeshUpdate
        clone.buffer = buffer
        clone.triBuffer = triBuffer
        clone.lineBuffer = lineBuffer
        clone.debugLineBuffer = debugLineBuffer
        clone.hasUVs = hasUVs
        clone.hasVertexColors = hasVertexColors
        clone.hasBonesInBuffer = hasBonesInBuffer
        clone.helperMeshes = helperMeshes
        clone.hasHighPrecisionNormals = hasHighPrecisionNormals
        // aabb
        clone.aabb.set(aabb)
        clone.ignoreStrayPointsInAABB = ignoreStrayPointsInAABB
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun readSomething(name: String, value: Any?) {
        if (!readSerializableProperty(name, value)) {
            super.readSomething(name, value)
        }
    }

    override val className = "Mesh"
    override val approxSize: Int = 1

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
            for (index in positions.indices step 3) {
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
        NormalCalculator.checkNormals(positions, normals!!, indices)
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
    private var buffer: StaticBuffer? = null

    @NotSerializedProperty
    private var triBuffer: IndexBuffer? = null

    @NotSerializedProperty
    private var lineBuffer: IndexBuffer? = null

    @NotSerializedProperty
    var debugLineBuffer: IndexBuffer? = null

    @NotSerializedProperty
    private var invalidDebugLines = true

    fun forEachPoint(onlyFaces: Boolean, callback: (x: Float, y: Float, z: Float) -> Unit) {
        val positions = positions ?: return
        val indices = indices
        if (onlyFaces && indices != null) {
            for (i in indices.indices) {
                val ai = indices[i] * 3
                callback(positions[ai], positions[ai + 1], positions[ai + 2])
            }
        } else {
            var i = 0
            val size = positions.size - 2
            while (i < size) {
                callback(positions[i++], positions[i++], positions[i++])
            }
        }
    }

    fun forEachTriangle(callback: (a: Vector3f, b: Vector3f, c: Vector3f) -> Unit) {
        val a = JomlPools.vec3f.create()
        val b = JomlPools.vec3f.create()
        val c = JomlPools.vec3f.create()
        forEachTriangle(a, b, c, callback)
        JomlPools.vec3f.sub(3)
    }

    fun forEachTriangleIndex(callback: (a: Int, b: Int, c: Int) -> Unit) {
        val indices = indices
        if (indices == null) {
            for (i in 0 until positions!!.size / 9) {
                val i3 = i * 3
                callback(i3 + 0, i3 + 1, i3 + 2)
            }
        } else {
            for (i in indices.indices step 3) {
                val a = indices[i + 0]
                val b = indices[i + 1]
                val c = indices[i + 2]
                callback(a, b, c)
            }
        }
    }

    fun forEachSideIndex(callback: (a: Int, b: Int) -> Unit) {
        val indices = indices
        if (indices == null) {
            for (i in 0 until positions!!.size / 3) {
                val i3 = i * 3
                callback(i3, i3 + 1)
                callback(i3 + 1, i3 + 2)
                callback(i3 + 2, i3)
            }
        } else {
            for (i in 0 until indices.size - 2 step 3) {
                val a = indices[i]
                val b = indices[i + 1]
                val c = indices[i + 2]
                callback(a, b)
                callback(b, c)
                callback(c, a)
            }
        }
    }

    fun forEachTriangle(
        a: Vector3f,
        b: Vector3f,
        c: Vector3f,
        callback: (a: Vector3f, b: Vector3f, c: Vector3f) -> Unit
    ) {
        val positions = positions ?: return
        val indices = indices
        if (indices != null) {
            for (i in 0 until indices.size - 2 step 3) {
                a.set2(positions, indices[i] * 3)
                b.set2(positions, indices[i + 1] * 3)
                c.set2(positions, indices[i + 2] * 3)
                callback(a, b, c)
            }
        } else {
            var i = 0
            val s = positions.size - 8
            while (i < s) {
                a.set2(positions, i)
                b.set2(positions, i + 3)
                c.set2(positions, i + 6)
                callback(a, b, c)
                i += 9
            }
        }
    }

    fun forEachTriangle(
        a: Vector3d,
        b: Vector3d,
        c: Vector3d,
        callback: (a: Vector3d, b: Vector3d, c: Vector3d) -> Unit
    ) {
        val positions = positions ?: return
        val indices = indices
        if (indices != null) {
            for (i in 0 until indices.size - 2 step 3) {
                a.set2(positions, indices[i] * 3)
                b.set2(positions, indices[i + 1] * 3)
                c.set2(positions, indices[i + 2] * 3)
                callback(a, b, c)
            }
        } else {
            var i = 0
            val s = positions.size - 8
            while (i < s) {
                a.set2(positions, i)
                b.set2(positions, i + 3)
                c.set2(positions, i + 6)
                callback(a, b, c)
                i += 9
            }
        }
    }

    var hasUVs = false
    var hasVertexColors = false
    var hasBonesInBuffer = false

    val numTriangles get() = indices?.run { size / 3 } ?: positions?.run { size / 9 } ?: 0

    var hasHighPrecisionNormals = false

    /** can be set to false to use tangents as an additional data channel; notice the RGB[-1,1] limit though */
    var checkTangents = true

    fun ensureBounds() {
        synchronized(this) {
            if (proceduralLength <= 0 && needsBoundsUpdate) {
                needsBoundsUpdate = false
                calculateAABB()
            }
        }
    }

    /**
     * upload the data to the gpu, if it has changed
     * */
    fun ensureBuffer() {
        synchronized(this) {
            if (needsMeshUpdate) updateMesh()
            if (GFX.isGFXThread()) buffer?.ensureBuffer()
        }
    }

    fun hasBuffer() = !needsMeshUpdate

    fun ensureNorTanUVs() {

        // not the safest, but well...
        val positions = positions ?: return // throw RuntimeException("mesh has no positions")

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

        NormalCalculator.checkNormals(positions, normals, indices)
        if (hasUVs && checkTangents) TangentCalculator.checkTangents(positions, normals, tangents, uvs, indices)

    }

    private fun updateMesh() {

        if (proceduralLength > 0) return

        ensureBounds()

        needsMeshUpdate = false

        // not the safest, but well...
        val positions = positions ?: return // throw RuntimeException("mesh has no positions")

        ensureNorTanUVs()

        val normals = normals!!
        val tangents = tangents

        val uvs = uvs
        val hasUVs = hasUVs

        val colors = color0
        val boneWeights = boneWeights
        val boneIndices = boneIndices

        val vertexCount = positions.size / 3
        val indices = indices

        val hasBones = boneWeights != null && boneWeights.isNotEmpty()
        hasBonesInBuffer = hasBones

        // todo missing attributes cause issues
        // todo probably because the VAO state doesn't have information about color and such, and grabs null
        // we could update all shaders to only use the given subset of attributes,
        // but that would cause all shaders to require to know about the mesh,
        // and currently that makes things messy...
        // todo instead, we could allocate large buffers with default values for those... might be better...
        val hasColors = colors != null && colors.isNotEmpty()
        hasVertexColors = hasColors

        val hasHighPrecisionNormals = hasHighPrecisionNormals

        val attributes = arrayListOf(
            Attribute("coords", 3),
        )

        attributes += if (hasHighPrecisionNormals) {
            Attribute("normals", AttributeType.FLOAT, 3)
        } else {
            Attribute("normals", AttributeType.SINT8_NORM, 4)
        }

        if (hasUVs) {
            attributes += Attribute("uvs", 2)
            attributes += Attribute("tangents", AttributeType.SINT8_NORM, 4)
        }

        if (hasColors) {
            attributes += Attribute("colors", AttributeType.UINT8_NORM, 4)
        }

        if (hasBones) {
            attributes += Attribute("weights", AttributeType.UINT8_NORM, MAX_WEIGHTS)
            attributes += Attribute("indices", AttributeType.UINT8, MAX_WEIGHTS, true)
        }

        val buffer = replaceBuffer(attributes, vertexCount, buffer)
        this.buffer = buffer

        triBuffer = replaceBuffer(buffer, indices, triBuffer)
        triBuffer?.drawMode = drawMode

        for (i in 0 until vertexCount) {

            // upload all data of one vertex

            val i2 = i * 2
            val i3 = i * 3
            val i4 = i * 4

            buffer.put(positions[i3])
            buffer.put(positions[i3 + 1])
            buffer.put(positions[i3 + 2])

            if (hasHighPrecisionNormals) {
                buffer.put(normals[i3])
                buffer.put(normals[i3 + 1])
                buffer.put(normals[i3 + 2])
            } else {
                buffer.putByte(normals[i3])
                buffer.putByte(normals[i3 + 1])
                buffer.putByte(normals[i3 + 2])
                buffer.putByte(0) // alignment
            }

            if (hasUVs) {

                if (uvs != null && i2 + 1 < uvs.size) {
                    buffer.put(uvs[i2])
                    buffer.put(1f - uvs[i2 + 1]) // todo in the future flip the textures instead
                } else buffer.put(0f, 0f)

                if (tangents != null && i4 + 3 < tangents.size) {
                    buffer.putByte(tangents[i4])
                    buffer.putByte(tangents[i4 + 1])
                    buffer.putByte(tangents[i4 + 2])
                    buffer.putByte(tangents[i4 + 3])
                } else {
                    buffer.putByte(normals[i3])
                    buffer.putByte(normals[i3 + 1])
                    buffer.putByte(normals[i3 + 2])
                    buffer.putByte(127) // positive ^^
                }

            }

            if (hasColors) {
                if (colors != null && i < colors.size) {
                    val color = colors[i]
                    buffer.putByte(color.r().toByte())
                    buffer.putByte(color.g().toByte())
                    buffer.putByte(color.b().toByte())
                    buffer.putByte(color.a().toByte())
                } else buffer.putInt(-1)
            }

            // only works if MAX_WEIGHTS is four
            if (hasBones) {

                if (boneWeights != null && i4 + 3 < boneWeights.size) {
                    val w0 = max(boneWeights[i4], 1e-5f)
                    val w1 = boneWeights[i4 + 1]
                    val w2 = boneWeights[i4 + 2]
                    val w3 = boneWeights[i4 + 3]
                    val normalisation = 255f / (w0 + w1 + w2 + w3)
                    // var w0b = (w0 * normalisation).roundToInt()
                    val w1b = (w1 * normalisation).roundToInt()
                    val w2b = (w2 * normalisation).roundToInt()
                    val w3b = (w3 * normalisation).roundToInt()
                    val w0b = max(255 - (w1b + w2b + w3b), 0)
                    buffer.putByte(w0b.toByte())
                    buffer.putByte(w1b.toByte())
                    buffer.putByte(w2b.toByte())
                    buffer.putByte(w3b.toByte())
                } else {
                    buffer.putByte(-1)
                    buffer.putByte(0)
                    buffer.putByte(0)
                    buffer.putByte(0)
                }

                if (boneIndices != null && i4 + 3 < boneIndices.size) {
                    buffer.putByte(boneIndices[i4])
                    buffer.putByte(boneIndices[i4 + 1])
                    buffer.putByte(boneIndices[i4 + 2])
                    buffer.putByte(boneIndices[i4 + 3])
                } else {
                    buffer.putInt(0)
                }

            }

        }

        val materialIds = materialIds
        val first = materialIds?.firstOrNull() ?: 0
        val hasMultipleMaterials = materialIds != null && materialIds.any { it != first }
        if (hasMultipleMaterials) {
            createHelperMeshes(materialIds!!)
        } else {
            destroyHelperMeshes()
            numMaterials = 1
        }

        // LOGGER.info("Flags($name): size: ${buffer.vertexCount}, colors? $hasColors, uvs? $hasUVs, bones? $hasBones")

        // find regular lines
        lineIndices = lineIndices ?: FindLines.findLines(indices, positions)
        lineBuffer = replaceBuffer(buffer, lineIndices, lineBuffer)
        lineBuffer?.drawMode = GL_LINES

        invalidDebugLines = true

    }

    fun ensureDebugLines() {
        val buffer = buffer
        if (invalidDebugLines && buffer != null) {
            invalidDebugLines = false
            debugLineIndices = FindLines.getAllLines(indices, positions, debugLineIndices)
            debugLineBuffer = replaceBuffer(buffer, debugLineIndices, debugLineBuffer)
            debugLineBuffer?.drawMode = GL_LINES
        }
    }

    fun createHelperMeshes(materialIds: IntArray, init: Boolean = true) {
        // todo use the same geometry data buffers: allow different index buffers per buffer
        // lines, per-material, all-together
        // creating separate buffers on the gpu,
        // split indices / data, would be of advantage here
        val length = materialIds.maxOrNull()!! + 1
        if (length == 1) return
        if (drawMode != GL_TRIANGLES) throw IllegalStateException("Multi-material meshes only supported on triangle meshes; got $drawMode")
        if (length > 1000) throw IllegalStateException("Material Id must be less than 1000!")
        val helperMeshes = arrayOfNulls<HelperMesh>(length)
        val indices = indices
        for (materialId in 0 until length) {
            val numTriangles = materialIds.count { it == materialId }
            if (numTriangles > 0) {
                val helperIndices = IntArray(numTriangles * 3)
                var j = 0
                var i3 = 0
                if (indices == null) {
                    for (i in materialIds.indices) {
                        val id = materialIds[i]
                        if (id == materialId) {
                            helperIndices[j++] = i3++
                            helperIndices[j++] = i3++
                            helperIndices[j++] = i3++
                        } else i3 += 3
                    }
                } else {
                    if (indices.size != materialIds.size * 3)
                        throw IllegalStateException("Material IDs must be exactly 3x smaller than indices")
                    for (i in materialIds.indices) {
                        val id = materialIds[i]
                        if (id == materialId) {
                            helperIndices[j++] = indices[i3++]
                            helperIndices[j++] = indices[i3++]
                            helperIndices[j++] = indices[i3++]
                        } else i3 += 3
                    }
                }
                if (j != helperIndices.size) throw IllegalStateException("Ids must have changed while processing")
                val helper = HelperMesh(helperIndices)
                if (init) helper.init(this)
                helperMeshes[materialId] = helper
            }// else mesh not required
        }
        this.helperMeshes = helperMeshes
        numMaterials = length
    }

    private fun destroyHelperMeshes() {
        helperMeshes?.forEach { it?.destroy() }
        helperMeshes = null
    }

    override fun destroy() {
        super.destroy()
        // todo only if we were not cloned...
        destroyHelperMeshes()
        // destroy buffers
        buffer?.destroy()
        triBuffer?.destroy()
        lineBuffer?.destroy()
        debugLineBuffer?.destroy()
        buffer = null
        triBuffer = null
        debugLineBuffer = null
    }

    fun draw(shader: Shader, materialIndex: Int) {
        val proceduralLength = proceduralLength
        if (proceduralLength <= 0) {
            ensureBuffer()
            // respect the material index: only draw what belongs to the material
            val helperMeshes = helperMeshes
            when {
                helperMeshes != null && materialIndex in helperMeshes.indices -> {
                    val helperMesh = helperMeshes[materialIndex] ?: return
                    if (drawDebugLines) {
                        helperMesh.ensureDebugLines(this)
                        helperMesh.debugLineBuffer?.draw(shader)
                    } else {
                        helperMesh.triBuffer?.draw(shader)
                        helperMesh.lineBuffer?.draw(shader)
                    }
                }
                materialIndex == 0 -> {
                    if (drawDebugLines) {
                        ensureDebugLines()
                        debugLineBuffer?.draw(shader)
                    } else {
                        (triBuffer ?: buffer)?.draw(shader)
                        lineBuffer?.draw(shader)
                    }
                }
            }
        } else StaticBuffer.drawArraysNull(shader, drawMode, proceduralLength)
    }

    fun drawMeshPurely(shader: Shader) {
        ensureBuffer()
        (triBuffer ?: buffer)?.draw(shader)
        lineBuffer?.draw(shader)
    }

    fun drawDepth(shader: Shader) {
        // all materials are assumed to behave the same
        // when we have vertex shaders by material, this will become wrong...
        val proceduralLength = proceduralLength
        if (proceduralLength <= 0) {
            ensureBuffer()
            if (drawDebugLines) {
                debugLineBuffer?.draw(shader)
            } else {
                (triBuffer ?: buffer)?.draw(shader)
            }
        } else StaticBuffer.drawArraysNull(shader, drawMode, proceduralLength)
    }

    fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: Buffer) {
        if (proceduralLength <= 0) {
            GFX.check()
            ensureBuffer()
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
        ensureBounds()
        forEachPoint(onlyFaces) { x, y, z ->
            aabb.union(transform.transformProject(vf.set(x, y, z)))
        }
        return aabb
    }

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int,
        cameraPosition: Vector3d,
        worldScale: Double
    ): Int {
        pipeline.addMesh(this, Pipeline.sampleMeshComponent, entity, clickId)
        return clickId
    }

    companion object {

        val drawDebugLines get() = RenderView.currentInstance?.renderMode.run { this == RenderMode.LINES || this == RenderMode.LINES_MSAA }

        val defaultMaterial = Material()
        private val defaultMaterials = emptyList<FileReference>()
        private val LOGGER = LogManager.getLogger(Mesh::class)

        // custom attributes for shaders? idk...
        // will always be 4, so bone indices can be aligned
        const val MAX_WEIGHTS = 4

        private fun replaceBuffer(
            attributes: List<Attribute>,
            vertexCount: Int,
            oldValue: StaticBuffer?
        ): StaticBuffer {
            if (oldValue != null) {
                // offsets are compared, so they need to be consistent
                computeOffsets(attributes)
                computeOffsets(oldValue.attributes)
                if (oldValue.attributes == attributes && oldValue.vertexCount == vertexCount) {
                    oldValue.clear()
                    return oldValue
                } else {
                    oldValue.destroy()
                }
            }
            return StaticBuffer(attributes, vertexCount)
        }

        private fun replaceBuffer(base: Buffer, indices: IntArray?, oldValue: IndexBuffer?): IndexBuffer? {
            return if (indices != null) {
                if (oldValue != null) {
                    if (base === oldValue.base) {
                        oldValue.indices = indices
                        return oldValue
                    } else oldValue.destroy()
                }
                IndexBuffer(base, indices)
            } else {
                oldValue?.destroy()
                null
            }
        }
    }

}