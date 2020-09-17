package me.anno.objects.meshes.fbx.model

import me.anno.objects.meshes.fbx.structure.FBXNode

class FBXPose(data: FBXNode): FBXObject(data) {

    // number of pose nodes = NbPoseNodes

    val poseNodes = data["PoseNode"].map { PoseNode(it) }

    class PoseNode(data: FBXNode){
        val id = data.getProperty("Node") as Long
        val matrix = data.getM4x4("Matrix")!!
    }

}