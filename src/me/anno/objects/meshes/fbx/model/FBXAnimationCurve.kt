package me.anno.objects.meshes.fbx.model

import me.anno.objects.meshes.fbx.structure.FBXNode

class FBXAnimationCurve(node: FBXNode): FBXObject(node){
    val defaultValue = node.getProperty("Default") as Double? ?: 0.0
    val nanoTimes = node.getLongArray("KeyTime")!!
    val values = node.getFloatArray("KeyValueFloat")
    val attrFlags = node.getIntArray("KeyAttrFlags") // ??
    val attrDataFloat = node.getFloatArray("KeyAttrDataFloat") // ??
    val attrRefCount = node.getIntArray("KeyAttrRefCount") // ??
}