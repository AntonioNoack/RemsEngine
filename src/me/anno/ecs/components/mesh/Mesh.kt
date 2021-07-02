package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.mesh.assimp.Material
import org.joml.AABBf
import org.lwjgl.opengl.GL11.GL_TRIANGLES


// todo MeshComponent + MeshRenderComponent (+ AnimatableComponent) = animated skeleton

// todo ui components, because we want everything to be ecs -> reuse our existing stuff? maybe

// todo in a game, there are assets, so
// todo - we need to pack assets
// todo - it would be nice, if FileReferences could point to local files as well
// todo always ship the editor with the game? would make creating mods easier :)
// (and cheating, but there always will be cheaters, soo...)

// todo custom shading environment, so we can easily convert every shader into something clickable
// todo also make it deferred / forward/backward compatible



class Mesh: Component() {

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

    // one per triangle
    var materialIndices = IntArray(0)
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

    companion object {
        fun AABBf.clear() {
            minX = Float.POSITIVE_INFINITY
            minY = Float.POSITIVE_INFINITY
            minZ = Float.POSITIVE_INFINITY
            maxX = Float.NEGATIVE_INFINITY
            maxY = Float.NEGATIVE_INFINITY
            maxZ = Float.NEGATIVE_INFINITY
        }
    }

    override fun getClassName(): String = "MeshComponent"

    // far into the future:
    // todo instanced animations for hundrets of humans:
    // todo bake animations into textures, and use indices + weights

}