package me.anno.mesh.gltf.reader

import org.joml.Quaterniond
import org.joml.Vector3d

class Node(val id: Int) {

    var name: String? = null
    var parent: Node? = null
    var children: List<Int> = emptyList()

    var boneId = -1

    var translation: Vector3d? = null
    var rotation: Quaterniond? = null
    var scale: Vector3d? = null

    var skin = -1
    var mesh = -1

    override fun toString(): String {
        return "Node[$mesh,'$name',$children]"
    }
}