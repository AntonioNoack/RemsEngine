package me.anno.mesh.blender.impl

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("unused")
class BMaterial(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val id = inside("id") as BID

    val r = float("r", 1f)
    val g = float("g", 1f)
    val b = float("b", 1f)
    val a = float("a", 1f) // or alpha?

    val roughness = float("roughness", 1f)
    val metallic = float("metallic", 0f)

    val useNodes = byte(getOffset("use_nodes")) != 0.toByte()
    val nodeTree = if (useNodes) getPointer("*nodetree") as? BNodeTree else null

    var fileRef: FileReference = InvalidRef
}