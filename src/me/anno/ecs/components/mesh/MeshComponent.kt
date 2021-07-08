package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.IndexedStaticBuffer
import me.anno.gpu.shader.Shader
import org.joml.AABBf
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import kotlin.math.max


// todo MeshComponent + MeshRenderComponent (+ AnimatableComponent) = animated skeleton

// todo ui components, because we want everything to be ecs -> reuse our existing stuff? maybe

// todo in a game, there are assets, so
// todo - we need to pack assets
// todo - it would be nice, if FileReferences could point to local files as well
// todo always ship the editor with the game? would make creating mods easier :)
// (and cheating, but there always will be cheaters, soo...)

// todo custom shading environment, so we can easily convert every shader into something clickable
// todo also make it deferred / forward/backward compatible


class MeshComponent : Component() {

    // todo morph targets
    // normals? maybe
    // positions? yes

    // use buffers instead, so they can be uploaded directly? no, I like the strided thing...
    // but it may be more flexible... still in Java, FloatArrays are more comfortable
    // -> just use multiple setters for convenience

    var positions: FloatArray? = null
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
    var drawMode = GL_TRIANGLES

    // todo when will it be calculated?
    val aabb = AABBf()

    var ignoreStrayPointsInAABB = false
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
    }

    fun invalidate() {}

    fun calculateNormals(smooth: Boolean) {
        if (smooth) {
            // todo vertex interpolation: keep track of how many faces were used
        } else {
            // todo use the cross product to calculate the normals
            // todo what if it's indexed, and the indexing contradicts the smooth shading? rebuild the index buffer
        }
    }

    fun calculateTangents() {
        // todo calculate them somehow...
    }

    fun check() {
        // todo check whether all variables are set correctly
    }

    override val className get() = "MeshComponent"

    // far into the future:
    // todo instanced animations for hundrets of humans:
    // todo bake animations into textures, and use indices + weights

    var needsMeshUpdate = true
    var buffer: IndexedStaticBuffer? = null

    fun updateMesh() {

        needsMeshUpdate = false

        calculateAABB()

        // todo if normals are null, calculate them

        val positions = positions!!
        val normals = normals!!
        val uvs = uvs
        val colors = color0
        val weights = boneWeights
        val jointIndices = boneIndices

        val indices = indices!!

        val pointCount = positions.size / 3
        val buffer = IndexedStaticBuffer(attributes, pointCount, indices)
        for (i in 0 until pointCount) {

            // upload all data of one vertex

            buffer.put(positions[i * 3])
            buffer.put(positions[i * 3 + 1])
            buffer.put(positions[i * 3 + 2])

            if (uvs != null && uvs.size > i * 2 + 1) {
                buffer.put(uvs[i * 2])
                buffer.put(uvs[i * 2 + 1])
            } else {
                buffer.put(0f, 0f)
            }

            buffer.put(normals[i * 3])
            buffer.put(normals[i * 3 + 1])
            buffer.put(normals[i * 3 + 2])

            if (colors != null && colors.size > i * 4 + 3) {
                buffer.put(colors[i * 4])
                buffer.put(colors[i * 4 + 1])
                buffer.put(colors[i * 4 + 2])
                buffer.put(colors[i * 4 + 3])
            } else {
                buffer.put(1f, 1f, 1f, 1f)
            }

            if (weights != null && weights.isNotEmpty()) {
                val w0 = max(weights[i * 4], 1e-5f)
                val w1 = weights[i * 4 + 1]
                val w2 = weights[i * 4 + 2]
                val w3 = weights[i * 4 + 3]
                val normalisation = 1f / (w0 + w1 + w2 + w3)
                buffer.put(w0 * normalisation)
                buffer.put(w1 * normalisation)
                buffer.put(w2 * normalisation)
                buffer.put(w3 * normalisation)
            } else {
                buffer.put(1f, 0f, 0f, 0f)
            }

            if (jointIndices != null && jointIndices.isNotEmpty()) {
                buffer.putByte(jointIndices[i * 4])
                buffer.putByte(jointIndices[i * 4 + 1])
                buffer.putByte(jointIndices[i * 4 + 2])
                buffer.putByte(jointIndices[i * 4 + 3])
            } else {
                buffer.putInt(0)
            }

        }

        this.buffer = buffer

    }

    fun draw(shader: Shader, materialIndex: Int) {
        // todo respect the material index
        // upload the data to the gpu, if it has changed
        if (needsMeshUpdate) updateMesh()
        buffer?.draw(shader)
    }

    override fun onDestroy() {
        super.onDestroy()
        buffer?.destroy()
    }

    companion object {

        // custom attributes for shaders? idk...
        const val MAX_WEIGHTS = 4
        val attributes = listOf(
            Attribute("coords", 3),
            Attribute("uvs", 2),
            Attribute("normals", 3),
            Attribute("colors", 4),
            Attribute("weights", MAX_WEIGHTS),
            Attribute("indices", AttributeType.UINT8, MAX_WEIGHTS, true)
        )

        fun AABBf.clear() {
            minX = Float.POSITIVE_INFINITY
            minY = Float.POSITIVE_INFINITY
            minZ = Float.POSITIVE_INFINITY
            maxX = Float.NEGATIVE_INFINITY
            maxY = Float.NEGATIVE_INFINITY
            maxZ = Float.NEGATIVE_INFINITY
        }

    }

}