package me.anno.ecs.components.mesh

import me.anno.ecs.components.mesh.MeshComponent.Companion.clear
import me.anno.gpu.buffer.IndexedStaticBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.Shader
import me.anno.io.NamedSaveable
import me.anno.io.serialization.NotSerializedProperty
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.lwjgl.opengl.GL11
import kotlin.math.max
import kotlin.math.min

class Mesh: NamedSaveable() {

    // use buffers instead, so they can be uploaded directly? no, I like the strided thing...
    // but it may be more flexible... still in Java, FloatArrays are more comfortable
    // -> just use multiple setters for convenience

    @NotSerializedProperty
    private var needsMeshUpdate = true

    // todo also we need a renderer, which can handle morphing
    var positions: FloatArray? = null
    var morphTargets = ArrayList<MorphTarget>()

    var normals: FloatArray? = null
    var tangents: FloatArray? = null
    var uvs: FloatArray? = null

    // multiple colors? idk...
    var color0: FloatArray? = null

    var boneWeights: FloatArray? = null
    var boneIndices: ByteArray? = null

    // todo allow multiple materials? should make our life easier :), we just need to split the indices...
    var materials = ArrayList<Material>()
    var material: Material?
        get() = materials.getOrNull(0)
        set(value) {
            materials.clear()
            if (value != null) {
                materials.add(value)
            }
        }

    // one per triangle
    var materialIndices = IntArray(0)

    // todo sort them by material/shader, and create multiple buffers (or sub-buffers) for them
    var indices: IntArray? = null

    // to allow for quads, and strips and such
    var drawMode = GL11.GL_TRIANGLES

    // todo when will it be calculated?
    val aabb = AABBf()

    var ignoreStrayPointsInAABB = false

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

    fun calculateTangents() {
        // todo calculate them somehow...
    }

    /**
     * throws an IllegalStateException, if anything is incorrectly set up
     * if this succeeds, then the drawing routine should not crash
     * */
    fun check() {
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
            if (boneWeights.size * 3 != positions.size * MeshComponent.MAX_WEIGHTS) throw IllegalStateException("Size of weights does not match positions, there must be ${MeshComponent.MAX_WEIGHTS} weights per vertex")
        }
        val color0 = color0
        if (color0 != null && color0.size * 3 != positions.size * 4) throw IllegalStateException("Every vertex needs an RGBA color value")
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

    private fun updateMesh() {

        needsMeshUpdate = false

        calculateAABB()

        val positions = positions!!
        if (normals == null)
            normals = FloatArray(positions.size)

        // if normals are null or have length 0, calculate them
        NormalCalculator.checkNormals(positions, normals!!, indices)

        val normals = normals!!
        val uvs = uvs
        val colors = color0
        val boneWeights = boneWeights
        val boneIndices = boneIndices

        val pointCount = positions.size / 3
        val indices = indices
        val buffer =
            if (indices == null) StaticBuffer(MeshComponent.attributes, pointCount)
            else IndexedStaticBuffer(MeshComponent.attributes, pointCount, indices)

        val hasBones = boneWeights != null && boneIndices != null
        val boneCount = if (hasBones) min(boneWeights!!.size, boneIndices!!.size) else 0

        for (i in 0 until pointCount) {

            // upload all data of one vertex

            val i2 = i * 2
            val i3 = i * 3
            val i4 = i * 4

            buffer.put(positions[i3])
            buffer.put(positions[i3 + 1])
            buffer.put(positions[i3 + 2])

            if (uvs != null && uvs.size > i2 + 1) {
                buffer.put(uvs[i2])
                buffer.put(uvs[i2 + 1])
            } else {
                buffer.put(0f, 0f)
            }

            buffer.put(normals[i3])
            buffer.put(normals[i3 + 1])
            buffer.put(normals[i3 + 2])

            if (colors != null && colors.size > i4 + 3) {
                buffer.put(colors[i4])
                buffer.put(colors[i4 + 1])
                buffer.put(colors[i4 + 2])
                buffer.put(colors[i4 + 3])
            } else {
                buffer.put(1f, 1f, 1f, 1f)
            }

            // only works if MAX_WEIGHTS is four
            if (boneWeights != null && boneWeights.isNotEmpty()) {
                val w0 = max(boneWeights[i4], 1e-5f)
                val w1 = boneWeights[i4 + 1]
                val w2 = boneWeights[i4 + 2]
                val w3 = boneWeights[i4 + 3]
                val normalisation = 1f / (w0 + w1 + w2 + w3)
                buffer.put(w0 * normalisation)
                buffer.put(w1 * normalisation)
                buffer.put(w2 * normalisation)
                buffer.put(w3 * normalisation)
            } else {
                buffer.put(1f, 0f, 0f, 0f)
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

        this.buffer?.destroy()
        this.buffer = buffer

    }

    fun ensureBuffer() {
        if (needsMeshUpdate) updateMesh()
    }

    fun draw(shader: Shader, materialIndex: Int) {
        // todo respect the material index
        // upload the data to the gpu, if it has changed
        ensureBuffer()
        buffer?.draw(shader)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Mesh::class)
    }

}