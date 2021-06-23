package me.anno.mesh.assimp

import me.anno.io.FileReference
import org.joml.Matrix4f
import org.joml.Vector4f


class VertexWeight(val boneId: Int, val vertexId: Int, val weight: Float) {

}

class Bone(val id: Int, val name: String, val offsetMatrix: Matrix4f) {

}

class Animation(val name: String, val frames: List<AnimatedFrame>, val duration: Double) {
    // var currentFrame = 0
}

class AnimatedFrame {
    val matrices = ArrayList<Matrix4f>()
    fun setMatrix(index: Int, matrix: Matrix4f) {
        for (i in matrices.size until index) {
            matrices.add(Matrix4f())
        }
        matrices[index] = matrix
    }
}

class Node(val name: String, val parentNode: Node?, val transformation: Matrix4f) {

    val children = ArrayList<Node>()
    fun addChild(child: Node) {
        children.add(child)
    }

    fun findByName(name: String): Node? {
        if (this.name == name) return this
        for (child in children) {
            val found = child.findByName(name)
            if (found != null) return found
        }
        return null
    }

}

class Material(val ambient: Vector4f, val diffuse: Vector4f, val specular: Vector4f, var alpha: Float) {

    constructor(): this(Vector4f(1f), Vector4f(1f), Vector4f(1f), 1f)

    var texture: FileReference? = null
    var normalMap: FileReference? = null

}