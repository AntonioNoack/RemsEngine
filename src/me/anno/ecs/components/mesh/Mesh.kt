package me.anno.ecs.components.mesh

import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.GFX
import me.anno.gpu.buffer.*
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.shader.Shader
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.FindLines
import me.anno.mesh.assimp.AnimGameItem
import me.anno.objects.GFXTransform
import me.anno.objects.meshes.MeshData
import me.anno.utils.types.AABBs.clear
import me.anno.utils.types.AABBs.set
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_LINES
import kotlin.math.max
import kotlin.math.roundToInt

class Mesh : PrefabSaveable() {

    // use buffers instead, so they can be uploaded directly? no, I like the strided thing...
    // but it may be more flexible... still in Java, FloatArrays are more comfortable
    // -> just use multiple setters for convenience

    @NotSerializedProperty
    private var needsMeshUpdate = true

    fun invalidateGeometry() {
        needsMeshUpdate = true
    }

    // todo also we need a renderer, which can handle morphing
    // todo or we need to compute it on the cpu
    @HideInInspector
    var positions: FloatArray? = null

    @Type("List<MorphTarget>")
    var morphTargets: List<MorphTarget> = emptyList()

    @HideInInspector
    var normals: FloatArray? = null

    @HideInInspector
    var tangents: FloatArray? = null

    @HideInInspector
    var uvs: FloatArray? = null

    // colors, rgba,
    // the default shader only supports the first color
    // other colors still can be loaded for ... idk... maybe terrain information or sth like that
    @HideInInspector
    var color0: IntArray? = null

    @HideInInspector
    var color1: IntArray? = null

    @HideInInspector
    var color2: IntArray? = null

    @HideInInspector
    var color3: IntArray? = null

    @HideInInspector
    var color4: IntArray? = null

    @HideInInspector
    var color5: IntArray? = null

    @HideInInspector
    var color6: IntArray? = null

    @HideInInspector
    var color7: IntArray? = null


    @HideInInspector
    var boneWeights: FloatArray? = null

    @HideInInspector
    var boneIndices: ByteArray? = null

    // todo find lines, and display them
    // triangle (a,b,c), where (a==b||b==c||c==a) && (a!=b||b!=c||c!=a)
    @HideInInspector
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
    var material: FileReference?
        get() = materials.getOrNull(0)
        set(value) {
            materials = if (value != null) listOf(value)
            else emptyList()
        }

    /**
     * one index per triangle
     * */
    var materialIndices: IntArray? = null

    var numMaterials = 1

    private var helperMeshes: Array<Mesh?>? = null

    // to allow for quads, and strips and such
    var drawMode = GL11.GL_TRIANGLES

    val aabb = AABBf()

    var ignoreStrayPointsInAABB = false

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Mesh
        ensureBuffer()
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
        clone.boneWeights = boneWeights
        clone.boneIndices = boneIndices
        clone.materialIndices = materialIndices
        // morph targets
        clone.morphTargets = morphTargets
        // draw mode
        clone.drawMode = drawMode
        // buffer
        clone.needsMeshUpdate = needsMeshUpdate
        clone.buffer = buffer
        clone.triBuffer = triBuffer
        clone.hasUVs = hasUVs
        clone.hasVertexColors = hasVertexColors
        clone.hasBonesInBuffer = hasBonesInBuffer
        clone.helperMeshes = helperMeshes
        // aabb
        clone.aabb.set(aabb)
        clone.ignoreStrayPointsInAABB = ignoreStrayPointsInAABB
    }

    override fun clone(): Mesh {
        ensureBuffer() // saves buffers
        val clone = Mesh()
        copy(clone)
        return clone
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

    override val className: String = "Mesh"
    override val approxSize: Int = 1

    fun calculateAABB() {
        aabb.clear()
        val indices = indices
        // if the indices array is empty, it indicates a non-indexed array, so all values will be considered
        if (ignoreStrayPointsInAABB && indices != null && indices.isNotEmpty()) {
            val positions = positions!!
            for (index in indices) {
                val x = positions[index * 3 + 0]
                val y = positions[index * 3 + 1]
                val z = positions[index * 3 + 2]
                aabb.union(x, y, z)
            }
        } else {
            val positions = positions!!
            for (index in positions.indices step 3) {
                val x = positions[index + 0]
                val y = positions[index + 1]
                val z = positions[index + 2]
                aabb.union(x, y, z)
            }
        }
        // LOGGER.info("Collected aabb $aabb from ${positions?.size}/${indices?.size} points")
    }

    fun calculateNormals(smooth: Boolean) {
        if (smooth && indices == null) LOGGER.warn("Meshes without indices cannot be rendered smoothly (for now)!")
        normals = FloatArray(positions!!.size)
        NormalCalculator.checkNormals(positions!!, normals!!, indices)
    }

    /**
     * throws an IllegalStateException, if anything is incorrectly set up
     * if this succeeds, then the drawing routine should not crash
     * */
    fun checkCompleteness() {
        // check whether all variables are set correctly
        val positions = positions
        val normals = normals
        val uvs = uvs
        if (positions == null) throw IllegalStateException("Missing positions")
        if (positions.size % 3 != 0) throw IllegalStateException("Positions must be a vector of vec3, but ${positions.size} % 3 != 0, it's ${positions.size % 3}")
        if (normals != null && normals.size != positions.size) throw IllegalStateException("Size of normals doesn't match size of positions")
        if (uvs != null) {
            if (uvs.size * 3 != positions.size * 2) throw IllegalStateException("Size of UVs does not match size of positions: ${positions.size}*2 vs ${uvs.size}*3")
        }
        val boneWeights = boneWeights
        val boneIndices = boneIndices
        if ((boneIndices == null) != (boneWeights == null)) throw IllegalStateException("Needs both or neither bone weights and indices")
        if (boneWeights != null && boneIndices != null) {
            if (boneWeights.size != boneIndices.size) throw IllegalStateException("Size of bone weights must match size of bone indices")
            if (boneWeights.size * 3 != positions.size * MAX_WEIGHTS) throw IllegalStateException("Size of weights does not match positions, there must be ${MAX_WEIGHTS} weights per vertex")
        }
        val color0 = color0
        if (color0 != null && color0.size * 3 != positions.size) throw IllegalStateException("Every vertex needs an ARGB color value")
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
        forEachTriangle(Vector3f(), Vector3f(), Vector3f(), callback)
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
                callback(i3 + 0, i3 + 1)
                callback(i3 + 1, i3 + 2)
                callback(i3 + 2, i3 + 0)
            }
        } else {
            for (i in indices.indices step 3) {
                val a = indices[i + 0]
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
            for (i in indices.indices step 3) {
                val ai = indices[i] * 3
                val bi = indices[i + 1] * 3
                val ci = indices[i + 2] * 3
                a.set(positions[ai], positions[ai + 1], positions[ai + 2])
                b.set(positions[bi], positions[bi + 1], positions[bi + 2])
                c.set(positions[ci], positions[ci + 1], positions[ci + 2])
                callback(a, b, c)
            }
        } else {
            var i = 0
            while (i + 8 < positions.size) {
                a.set(positions[i++], positions[i++], positions[i++])
                b.set(positions[i++], positions[i++], positions[i++])
                c.set(positions[i++], positions[i++], positions[i++])
                callback(a, b, c)
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
            for (i in indices.indices step 3) {
                val ai = indices[i] * 3
                val bi = indices[i + 1] * 3
                val ci = indices[i + 2] * 3
                a.set(positions[ai].toDouble(), positions[ai + 1].toDouble(), positions[ai + 2].toDouble())
                b.set(positions[bi].toDouble(), positions[bi + 1].toDouble(), positions[bi + 2].toDouble())
                c.set(positions[ci].toDouble(), positions[ci + 1].toDouble(), positions[ci + 2].toDouble())
                callback(a, b, c)
            }
        } else {
            var i = 0
            while (i + 8 < positions.size) {
                a.set(positions[i++].toDouble(), positions[i++].toDouble(), positions[i++].toDouble())
                b.set(positions[i++].toDouble(), positions[i++].toDouble(), positions[i++].toDouble())
                c.set(positions[i++].toDouble(), positions[i++].toDouble(), positions[i++].toDouble())
                callback(a, b, c)
            }
        }
    }

    var hasUVs = false
    var hasVertexColors = false
    var hasBonesInBuffer = false

    var drawLines = false

    val numTriangles get() = indices?.run { size / 3 } ?: positions?.run { size / 9 } ?: 0

    private fun updateMesh() {

        needsMeshUpdate = false

        calculateAABB()

        val positions = positions!!
        if (normals == null)
            normals = FloatArray(positions.size)

        if (tangents == null && uvs != null)
            tangents = FloatArray(positions.size)

        // if normals are null or have length 0, calculate them
        val normals = normals!!
        val tangents = tangents


        val uvs = uvs
        val hasUVs = uvs != null && uvs.isNotEmpty()
        this.hasUVs = hasUVs

        NormalCalculator.checkNormals(positions, normals, indices)
        if (hasUVs) TangentCalculator.checkTangents(positions, normals, tangents, uvs, indices)

        val colors = color0
        val boneWeights = boneWeights
        val boneIndices = boneIndices

        val pointCount = positions.size / 3
        val indices = indices

        val hasBones = boneWeights != null && boneWeights.isNotEmpty()
        hasBonesInBuffer = hasBones

        // todo missing attributes cause issues... why??
        val hasColors = true || colors != null && colors.isNotEmpty()
        hasVertexColors = hasColors


        val attributes = arrayListOf(
            Attribute("coords", 3),
            Attribute("normals", AttributeType.SINT8_NORM, 4),
        )

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

        // todo reuse the old buffer, if the size matches
        val buffer = StaticBuffer(attributes, pointCount)
        val triBuffer = if (indices != null) IndexBuffer(buffer, indices) else null

        for (i in 0 until pointCount) {

            // upload all data of one vertex

            val i2 = i * 2
            val i3 = i * 3
            val i4 = i * 4

            buffer.put(positions[i3])
            buffer.put(positions[i3 + 1])
            buffer.put(positions[i3 + 2])

            buffer.putByte(normals[i3])
            buffer.putByte(normals[i3 + 1])
            buffer.putByte(normals[i3 + 2])
            buffer.putByte(0) // alignment

            if (hasUVs) {

                if (uvs != null && uvs.size > i2 + 1) {
                    buffer.put(uvs[i2])
                    buffer.put(1f - uvs[i2 + 1]) // todo in the future flip the textures instead
                } else buffer.put(0f, 0f)

                if (tangents != null) {
                    buffer.putByte(tangents[i3])
                    buffer.putByte(tangents[i3 + 1])
                    buffer.putByte(tangents[i3 + 2])
                    buffer.putByte(0) // alignment
                } else buffer.putInt(0)

            }

            if (hasColors) {
                if (colors != null && colors.size > i) {
                    buffer.putInt(colors[i])
                } else buffer.putInt(-1)
            }

            // only works if MAX_WEIGHTS is four
            if (hasBones) {

                if (boneWeights != null && boneWeights.isNotEmpty()) {
                    val w0 = max(boneWeights[i4], 1e-5f)
                    val w1 = boneWeights[i4 + 1]
                    val w2 = boneWeights[i4 + 2]
                    val w3 = boneWeights[i4 + 3]
                    val normalisation = 255f / (w0 + w1 + w2 + w3)
                    // var w0b = (w0 * normalisation).roundToInt()
                    val w1b = (w1 * normalisation).roundToInt()
                    val w2b = (w2 * normalisation).roundToInt()
                    val w3b = (w3 * normalisation).roundToInt()
                    val w0b = 255 - (w1b + w2b + w3b) // should be positive
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

                if (boneIndices != null && boneIndices.isNotEmpty()) {
                    buffer.putByte(boneIndices[i4])
                    buffer.putByte(boneIndices[i4 + 1])
                    buffer.putByte(boneIndices[i4 + 2])
                    buffer.putByte(boneIndices[i4 + 3])
                } else {
                    buffer.putInt(0)
                }

            }

        }

        val materialIndices = materialIndices
        val first = materialIndices?.firstOrNull() ?: 0
        val hasMultipleMaterials = materialIndices != null && materialIndices.any { it != first && it >= 0 }
        if (hasMultipleMaterials) {
            createHelperMeshes(materialIndices!!)
        } else {
            destroyHelperMeshes()
            numMaterials = 1
        }

        // LOGGER.info("Flags($name): size: ${buffer.vertexCount}, colors? $hasColors, uvs? $hasUVs, bones? $hasBones")

        val lineBuffer = if (drawLines) {
            val lineIndices = lineIndices ?: FindLines.findLines(indices, positions)
            if (lineIndices != null) {
                this.lineIndices = lineIndices
                // todo reuse old buffer as well
                val lineBuffer = IndexBuffer(buffer, lineIndices)
                lineBuffer.drawMode = GL_LINES
                lineBuffer
            } else null
        } else null

        this.lineBuffer?.destroy()
        this.lineBuffer = lineBuffer

        this.buffer?.destroy()
        this.buffer = buffer
        this.triBuffer?.destroy()
        this.triBuffer = triBuffer

    }

    private fun createHelperMeshes(materialIndices: IntArray) {
        // todo use the same geometry data buffers: allow different index buffers per buffer
        // lines, per-material, all-together
        // creating separate buffers on the gpu,
        // split indices / data, would be of advantage here
        val length = materialIndices.maxOrNull()!! + 1
        if (length == 1) return
        if (drawMode != GL11.GL_TRIANGLES) throw IllegalStateException("Multi-material meshes only supported on triangle meshes; got $drawMode")
        if (length > 1000) throw IllegalStateException("Material Id must be less than 1000!")
        val helperMeshes = arrayOfNulls<Mesh>(length)
        val indices = indices
        val materials = materials
        for (materialId in 0 until length) {
            if (materialIndices.any { it == materialId }) {
                val mesh = Mesh()
                copy(mesh)
                mesh.material = materials.getOrNull(materialId)
                mesh.materialIndices = null
                val triangles = materialIndices.count { it == materialId }
                val partialIndices = IntArray(triangles * 3)
                var j = 0
                if (indices == null) {
                    for (i in materialIndices.indices) {
                        val id = materialIndices[i]
                        if (id == materialId) {
                            val i3 = i * 3
                            partialIndices[j++] = i3
                            partialIndices[j++] = i3 + 1
                            partialIndices[j++] = i3 + 2
                        }
                    }
                } else {
                    for (i in materialIndices.indices) {
                        val id = materialIndices[i]
                        if (id == materialId) {
                            val i3 = i * 3
                            partialIndices[j++] = indices[i3]
                            partialIndices[j++] = indices[i3 + 1]
                            partialIndices[j++] = indices[i3 + 2]
                        }
                    }
                }
                mesh.indices = partialIndices
                mesh.checkCompleteness()
                mesh.invalidateGeometry()
                helperMeshes[materialId] = mesh
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
        // todo destroy buffer?
    }

    /**
     * upload the data to the gpu, if it has changed
     * */
    fun ensureBuffer() {
        synchronized(this) {
            if (needsMeshUpdate) updateMesh()
        }
    }

    fun draw(shader: Shader, materialIndex: Int) {
        ensureBuffer()
        // respect the material index: only draw what belongs to the material
        val helperMeshes = helperMeshes
        if (helperMeshes != null) {
            helperMeshes
                .getOrNull(materialIndex)
                ?.draw(shader, 0)
        } else {
            if (materialIndex == 0) {
                (triBuffer ?: buffer)?.draw(shader)
                lineBuffer?.draw(shader)
            }
        }
    }

    /*fun drawLines(shader: Shader, materialIndex: Int) {
        ensureBuffer()
        lineBuffer?.draw(shader)
    }*/

    fun drawDepth(shader: Shader) {
        // all materials are assumed to behave the same
        // when we have vertex shaders by material, this will become wrong...
        ensureBuffer()
        // buffer?.draw(shader)
        (triBuffer ?: buffer)?.draw(shader)
    }

    fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: Buffer) {
        GFX.check()
        ensureBuffer()
        // respect the material index: only draw what belongs to the material
        val helperMeshes = helperMeshes
        if (helperMeshes != null) {
            helperMeshes
                .getOrNull(materialIndex)
                ?.drawInstanced(shader, 0, instanceData)
        } else {
            if (materialIndex == 0) {
                (triBuffer ?: buffer)?.drawInstanced(shader, instanceData)
            }
        }
        GFX.check()
    }

    fun drawInstancedDepth(shader: Shader, instanceData: Buffer) {
        // draw all materials
        GFX.check()
        ensureBuffer()
        (triBuffer ?: buffer)?.drawInstanced(shader, instanceData)
        GFX.check()
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
        ensureBuffer()
        forEachPoint(onlyFaces) { x, y, z ->
            aabb.union(transform.transformProject(vf.set(x, y, z)))
        }
        return aabb
    }

    fun drawAssimp(
        stack: Matrix4f,
        useMaterials: Boolean,
        centerMesh: Boolean,
        normalizeScale: Boolean
    ) {

        val shader = ECSShaderLib.pbrModelShader.value
        shader.use()
        GFXx3D.shader3DUniforms(shader, stack, -1)
        GFXTransform.uploadAttractors(null, shader, 0.0)

        val localStack = if (normalizeScale || centerMesh) {
            val localStack = Matrix4x3f()
            if (normalizeScale) {
                val scale = AnimGameItem.getScaleFromAABB(aabb)
                localStack.scale(scale)
            }
            if (centerMesh) {
                MeshData.centerMesh(stack, localStack, this)
            }
            localStack
        } else null

        shader.v1("hasAnimation", false)
        shader.m4x4("transform", stack)
        shader.m4x3("localTransform", localStack)

        val mesh = this
        val materials = materials

        GFX.shaderColor(shader, "tint", -1)

        if (useMaterials && materials.isNotEmpty()) {
            for (materialIndex in materials.indices) {
                val materialRef = materials[materialIndex]
                val material = MaterialCache[materialRef, defaultMaterial]
                material.defineShader(shader)
                mesh.draw(shader, materialIndex)
            }
        } else {
            val material = defaultMaterial
            material.defineShader(shader)
            for (materialIndex in 0 until max(1, materials.size)) {
                mesh.draw(shader, materialIndex)
            }
        }
    }

    companion object {

        val defaultMaterial = Material()
        private val defaultMaterials = emptyList<FileReference>()
        private val LOGGER = LogManager.getLogger(Mesh::class)

        // custom attributes for shaders? idk...
        // will always be 4, so bone indices can be aligned
        const val MAX_WEIGHTS = 4

    }

}