package me.anno.mesh.blender.impl

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.nodes.BNodeTree

@Suppress("unused")
class BMaterial(ptr: ConstructorData) : BlendData(ptr) {

    val id = inside("id") as BID

    val r = f32("r", 1f)
    val g = f32("g", 1f)
    val b = f32("b", 1f)
    val a = f32("a", 1f) // or alpha?

    val roughness = f32("roughness", 1f)
    val metallic = f32("metallic", 0f)

    val useNodes = i8("use_nodes") != 0.toByte()
    val nodeTree = if (useNodes) getPointer("*nodetree") as? BNodeTree else null

    var fileRef: FileReference = InvalidRef
}