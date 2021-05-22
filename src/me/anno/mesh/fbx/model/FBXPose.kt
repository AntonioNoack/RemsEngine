package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode

class FBXPose(data: FBXNode): FBXObject(data) {

    // number of pose nodes = NbPoseNodes
    val poseNodes = data.mapAll("PoseNode") { PoseNode(it) }

    class PoseNode(data: FBXNode){
        val id = data.getProperty("Node") as Long
        val matrix = data.getM4x4("Matrix")!!
    }

}