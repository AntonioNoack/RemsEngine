package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection", "unused")
class BNodeTree(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val nodes = inside("nodes") as BListBase
    val links = inside("links") as BListBase
    val inputs = inside("inputs") as BListBase
    val outputs = inside("outputs") as BListBase

    val nodeType = int("nodetype")

}