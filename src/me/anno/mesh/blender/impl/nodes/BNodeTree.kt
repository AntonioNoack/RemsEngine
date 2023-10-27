package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.mesh.blender.impl.BListBase
import me.anno.mesh.blender.impl.BlendData
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_node_types.h
 * */
@Suppress("unused", "SpellCheckingInspection", "UNCHECKED_CAST")
class BNodeTree(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val nodes = inside("nodes") as BListBase<BNode>
    val links = inside("links") as BListBase<BNodeLink>

    // inputs and ouputs are deprecated; find them by type

    val type = string("idname[64]", 64)

    override fun toString(): String {
        return "bNodeTree { $type, nodes: [\n${
                    nodes.joinToString("") { "  $it\n" }
                }], links: [\n${
                    links.joinToString("") { "  $it\n" }
                }] }"
    }
}