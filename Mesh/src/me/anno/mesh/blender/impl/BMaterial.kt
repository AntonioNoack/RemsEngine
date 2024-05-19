package me.anno.mesh.blender.impl

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.nodes.BNodeTree

@Suppress("unused")
class BMaterial(ptr: ConstructorData) : BlendData(ptr) {

    val id = inside("id") as BID

    val r = float("r", 1f)
    val g = float("g", 1f)
    val b = float("b", 1f)
    val a = float("a", 1f) // or alpha?

    val roughness = float("roughness", 1f)
    val metallic = float("metallic", 0f)

    val useNodes = byte("use_nodes") != 0.toByte()
    val nodeTree = if (useNodes) getPointer("*nodetree") as? BNodeTree else null

    var fileRef: FileReference = InvalidRef
}