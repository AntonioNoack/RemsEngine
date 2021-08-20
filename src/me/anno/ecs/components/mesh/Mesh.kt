package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Type
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.GFX
import me.anno.gpu.TextureLib
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.IndexedStaticBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.shader.Shader
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializedProperty
import me.anno.mesh.assimp.AnimGameItem
import me.anno.objects.GFXTransform
import me.anno.utils.types.AABBs.clear
import me.anno.utils.types.AABBs.set
import me.anno.utils.types.Lists.asMutableList
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.opengl.GL11
import kotlin.math.max
import kotlin.math.roundToInt

// todo make main.json always the main scene in a mesh file, and don't load everything twice...

class Mesh : Component(), Cloneable {

    // use buffers instead, so they can be uploaded directly? no, I like the strided thing...
    // but it may be more flexible... still in Java, FloatArrays are more comfortable
    // -> just use multiple setters for convenience

    @NotSerializedProperty
    private var needsMeshUpdate = true

    var isInstanced = false

    var collisionMask: Int = 1

    fun canCollide(collisionMask: Int): Boolean {
        return this.collisionMask.and(collisionMask) != 0
    }

    // todo also we need a renderer, which can handle morphing
    @HideInInspector
    var positions: FloatArray? = null

    @Type("List<MorphTarget>")
    var morphTargets = ArrayList<MorphTarget>()

    @HideInInspector
    var normals: FloatArray? = null

    @HideInInspector
    var tangents: FloatArray? = null

    @HideInInspector
    var uvs: FloatArray? = null

    // multiple colors? idk...
    // force RGBA? typically that would be the use for it -> yes
    @HideInInspector
    var color0: IntArray? = null

    @HideInInspector
    var boneWeights: FloatArray? = null

    @HideInInspector
    var boneIndices: ByteArray? = null

    // todo allow multiple materials? should make our life easier :), we just need to split the indices...
    @Type("List<Material>")
    var materials: List<Material> = defaultMaterials

    @Type("Material")
    var material: Material?
        get() = materials.getOrNull(0)
        set(value) {
            materials = if (value != null) listOf(value)
            else emptyList()
        }

    // one per triangle
    var materialIndices = IntArray(0)

    // todo sort them by material/shader, and create multiple buffers (or sub-buffers) for them
    @HideInInspector
    var indices: IntArray? = null

    // to allow for quads, and strips and such
    var drawMode = GL11.GL_TRIANGLES

    // todo when will it be calculated?
    val aabb = AABBf()

    var ignoreStrayPointsInAABB = false

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as Mesh
        ensureBuffer()
        clone.buffer = buffer
        clone.hasBonesInBuffer = hasBonesInBuffer
        clone.isInstanced = isInstanced
        clone.collisionMask = collisionMask
        clone.materials = materials
        clone.positions = positions
        clone.normals = normals
        clone.aabb.set(aabb)
        clone.uvs = uvs
        clone.color0 = color0
        clone.tangents = tangents
        clone.indices = indices
        clone.boneWeights = boneWeights
        clone.boneIndices = boneIndices
        clone.morphTargets = morphTargets
        clone.ignoreStrayPointsInAABB = ignoreStrayPointsInAABB
        clone.drawMode = drawMode
        clone.materialIndices = materialIndices
        clone.needsMeshUpdate = needsMeshUpdate
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

    fun forEachTriangle(callback: (a: Vector3f, b: Vector3f, c: Vector3f) -> Unit) {
        forEachTriangle(Vector3f(), Vector3f(), Vector3f(), callback)
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

    var hasVertexColors = false
    var hasBonesInBuffer = false

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

        NormalCalculator.checkNormals(positions, normals, indices)
        if (hasUVs) TangentCalculator.checkTangents(positions, normals, tangents, uvs, indices)

        val colors = color0
        val boneWeights = boneWeights
        val boneIndices = boneIndices

        val pointCount = positions.size / 3
        val indices = indices

        val hasBones = boneWeights != null && boneWeights.isNotEmpty()
        hasBonesInBuffer = hasBones

        val hasColors = colors != null && colors.isNotEmpty()
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

        val buffer =
            if (indices == null) StaticBuffer(attributes, pointCount)
            else IndexedStaticBuffer(attributes, pointCount, indices)

        // val hasBones = boneWeights != null && boneIndices != null
        // val boneCount = if (hasBones) min(boneWeights!!.size, boneIndices!!.size) else 0

        // to do only put the attributes, which are really available, and push only them
        // our shader ofc would need to cope with missing attributes

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

                uvs!!
                if (uvs.size > i2 + 1) {
                    buffer.put(uvs[i2], uvs[i2 + 1])
                } else buffer.put(0f, 0f)

                if (tangents != null) {
                    buffer.putByte(tangents[i3])
                    buffer.putByte(tangents[i3 + 1])
                    buffer.putByte(tangents[i3 + 2])
                    buffer.putByte(0) // alignment
                } else buffer.putInt(0)

            }

            if (hasColors) {
                colors!!
                if (colors.size > i) {
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

        LOGGER.info("Flags($name): size: ${buffer.vertexCount}, colors? $hasColors, uvs? $hasUVs, bones? $hasBones")

        this.buffer?.destroy()
        this.buffer = buffer

    }

    /**
     * upload the data to the gpu, if it has changed
     * */
    fun ensureBuffer() {
        if (needsMeshUpdate) updateMesh()
    }

    fun draw(shader: Shader, materialIndex: Int) {
        // todo respect the material index: only draw what belongs to the material
        ensureBuffer()
        buffer?.draw(shader)
    }

    fun drawInstanced(shader: Shader, materialIndex: Int, instanceData: StaticBuffer) {
        // todo respect the material index: only draw what belongs to the material
        ensureBuffer()
        val meshBuffer = buffer
        if (meshBuffer != null) {
            instanceData.drawInstanced(shader, meshBuffer)
        }
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
                AnimGameItem.centerStackFromAABB(localStack, aabb)
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
            for (i in materials.indices) {
                val material = materials[i]
                material.defineShader(shader)
                mesh.draw(shader, i)
            }
        } else {
            TextureLib.whiteTexture.bind(0)
            mesh.draw(shader, 0)
        }

    }

    override fun getChildListNiceName(type: Char): String = "Materials"
    override fun listChildTypes(): String = "m"
    override fun getChildListByType(type: Char): List<PrefabSaveable> = materials
    override fun addChildByType(index: Int, type: Char, instance: PrefabSaveable) {
        if (instance !is Material) return
        val ms = materials.asMutableList()
        ms.add(instance)
        materials = ms
    }

    override fun removeChild(child: PrefabSaveable) {
        if (child !is Material) return
        if (materials.size <= 1) {
            materials = emptyList()
        } else if (child in materials) {
            val ms = materials.asMutableList()
            ms.remove(child)
            materials = ms
        }
    }

    companion object {

        private val defaultMaterials = listOf(Material())
        private val LOGGER = LogManager.getLogger(Mesh::class)

        // custom attributes for shaders? idk...
        // will always be 4, so bone indices can be aligned
        const val MAX_WEIGHTS = 4

        val attributesBoneless = listOf(
            Attribute("coords", 3),
            Attribute("uvs", 2), // 20 bytes
            Attribute("normals", AttributeType.SINT8_NORM, 4),
            Attribute("tangents", AttributeType.SINT8_NORM, 4),
            Attribute("colors", AttributeType.UINT8_NORM, 4), // 28 + 4 bytes
        )

        val attributes = listOf(
            Attribute("coords", 3),
            Attribute("uvs", 2), // 20 bytes
            Attribute("normals", AttributeType.SINT8_NORM, 4),
            Attribute("tangents", AttributeType.SINT8_NORM, 4),
            Attribute("colors", AttributeType.UINT8_NORM, 4), // 28 + 4 bytes
            Attribute("weights", AttributeType.UINT8_NORM, MAX_WEIGHTS),
            Attribute("indices", AttributeType.UINT8, MAX_WEIGHTS, true) // 32 + 8 bytes
        )

    }

}