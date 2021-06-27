package me.anno.mesh.assimp

import org.joml.Matrix4f

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