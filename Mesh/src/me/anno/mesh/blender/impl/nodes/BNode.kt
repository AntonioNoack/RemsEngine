package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.mesh.blender.impl.BLink
import me.anno.mesh.blender.impl.BListBase
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
@Suppress("unused", "SpellCheckingInspection", "UNCHECKED_CAST")
class BNode(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<BNode>(file, type, buffer, position) {

    val name = string("name[64]", 64)
    val type = string("idname[64]", 64)

    val id = getPointer("*id")

    val editorX = float("locx")
    val editorY = float("locy")
    val editorWidth = float("width")
    val editorHeigth = float("height")

    val inputs = (inside("inputs") as BListBase<BNodeSocket>).toList()
    val outputs = (inside("outputs") as BListBase<BNodeSocket>).toList()

    // texture settings for ShaderNodeTexImage
    // val storage = getPointer("*storage")

    fun findInputSocket(name: String): BNodeSocket? {
        return inputs.firstOrNull { it.name == name }
    }

    fun findOutputSocket(name: String): BNodeSocket? {
        return outputs.firstOrNull { it.name == name }
    }

    override fun toString(): String {
        return "bNode@${position.toString(16)} { $id, '$name', $type, " +
                "size: $editorX,$editorY+=${editorWidth}x${editorHeigth}, " +
                "inputs: $inputs, outputs: $outputs }"
    }
}