package me.anno.mesh.gltf.reader

import org.joml.Matrix4x3f
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

    val globalJointTransform = Matrix4x3f()

    var skin = -1
    var mesh = -1

    fun calculateGlobalTransform() {
        val parent = parent
        parent?.calculateGlobalTransform()

        if (parent != null) globalJointTransform.set(parent.globalJointTransform)
        else globalJointTransform.identity()

        val translation = translation
        val rotation = rotation
        val scale = scale
        if (translation != null) globalJointTransform.translate(
            translation.x.toFloat(),
            translation.y.toFloat(),
            translation.z.toFloat()
        )
        if (rotation != null) globalJointTransform.rotate(
            rotation.x.toFloat(),
            rotation.y.toFloat(),
            rotation.z.toFloat(),
            rotation.w.toFloat()
        )
        if (scale != null) globalJointTransform.scale(
            scale.x.toFloat(),
            scale.y.toFloat(),
            scale.z.toFloat()
        )
    }

    override fun toString(): String {
        return "Node[$mesh,'$name',$children]"
    }
}