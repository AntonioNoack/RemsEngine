package me.anno.mesh.gltf.reader

import me.anno.ecs.prefab.change.Path
import org.joml.Matrix4x3f
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

class Node(val id: Int) {

    var name: String? = null
    var parent: Node? = null
    var children: List<Int> = emptyList()

    var boneId = -1

    var globalTransform: Matrix4x3? = null // used for FlatScene.json
    var translation: Vector3d? = null
    var rotation: Quaternionf? = null
    var scale: Vector3f? = null

    val globalJointTransform = Matrix4x3f() // used for animation-calculations

    var skin = -1
    var mesh = -1

    var hasMeshes = false // used to remove nodes without meshes

    lateinit var path: Path // used when writing Scene.json

    override fun toString(): String {
        return "Node[$mesh,'$name',$children]"
    }
}