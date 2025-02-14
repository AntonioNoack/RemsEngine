package me.anno.mesh.gltf.reader

import me.anno.ecs.prefab.change.Path
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Vector3d

class Node(val id: Int) {

    var name: String? = null
    var parent: Node? = null
    var children: List<Int> = emptyList()

    var boneId = -1

    var globalTransform: Matrix4x3d? = null // used for FlatScene.json
    var translation: Vector3d? = null
    var rotation: Quaterniond? = null
    var scale: Vector3d? = null

    val globalJointTransform = Matrix4x3f() // used for animation-calculations

    var skin = -1
    var mesh = -1

    var hasMeshes = false // used to remove nodes without meshes

    lateinit var path: Path // used when writing Scene.json

    override fun toString(): String {
        return "Node[$mesh,'$name',$children]"
    }
}