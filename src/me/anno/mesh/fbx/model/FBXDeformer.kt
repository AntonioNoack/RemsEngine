package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode
import org.joml.Matrix4f

class FBXDeformer(data: FBXNode): FBXObject(data){

    // appears once and has spelling mistake (wtf xD)
    // version 101 instead of 100
    // val linkDeformAccuracy = data["Link_DeformAccuracy"]

    var parent: FBXDeformer? = null
    var index = 0
    val depth: Int get() = (parent?.depth ?: -1) + 1

    val mode = data.getProperty("Mode") as? String // Total1 or
    val transform = data.getM4x4("Transform") // "refers to the global initial position of the node containing the link"
    val transformLink = data.getM4x4("TransformLink") // "refers to global initial position of the link node"

    val indices = data.getIntArray("Indexes")
    val weights = data.getDoubleArray("Weights")

    var localJointMatrix = Matrix4f()

    var dx = 0f
    var dy = 0f
    var dz = 0f

    // override fun toString(depth: Int) = Tabs.spaces(depth*2) + "$depth ${javaClass.simpleName.substring(3)}:$name:$subType [${children.size}]\n"

}