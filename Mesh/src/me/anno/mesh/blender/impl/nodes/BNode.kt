package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BLink
import me.anno.mesh.blender.impl.BListBase

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
@Suppress("unused", "SpellCheckingInspection", "UNCHECKED_CAST")
class BNode(ptr: ConstructorData) : BLink<BNode>(ptr) {

    val name = string("name[64]", 64)
    val type = string("idname[64]", 64)

    val id = getPointer("*id")

    val editorX = f32("locx")
    val editorY = f32("locy")
    val editorWidth = f32("width")
    val editorHeigth = f32("height")

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