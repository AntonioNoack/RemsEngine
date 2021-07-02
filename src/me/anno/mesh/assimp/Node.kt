package me.anno.mesh.assimp

import org.joml.Matrix4f

class Node(val name: String, val parentNode: Node?, val transformation: Matrix4f) {

    val children = ArrayList<Node>()
    fun addChild(child: Node) {
        children.add(child)
    }

}