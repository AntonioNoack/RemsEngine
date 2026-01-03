package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BListBase
import me.anno.mesh.blender.impl.BlendData

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
@Suppress("unused", "SpellCheckingInspection", "UNCHECKED_CAST")
class BNodeTree(ptr: ConstructorData) : BlendData(ptr) {

    val nodes = inside("nodes") as BListBase<BNode>
    val links = inside("links") as BListBase<BNodeLink>

    // inputs and ouputs are deprecated; find them by type

    val type = string("idname[64]", 64)

    override fun toString(): String {
        return "bNodeTree { $type, nodes: ${nodes.size}x [\n${
            nodes.joinToString("") { "  $it\n" }
        }], links: ${links.size} [\n${
            links.joinToString("") { "  $it\n" }
        }] }"
    }
}