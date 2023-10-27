package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.mesh.blender.impl.BLink
import java.nio.ByteBuffer

@Suppress("unused", "SpellCheckingInspection")
class BNodeLink(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<BNodeLink>(file, type, buffer, position) {

    val fromNode = getPointer("*fromnode") as BNode
    val fromSocket = getPointer("*fromsock") as BNodeSocket
    val toNode = getPointer("*tonode") as BNode
    val toSocket = getPointer("*tosock") as BNodeSocket

    // flag, multi_input_socket_index

    override fun toString(): String {
        return "bNodeLink { " +
                "from: ${fromNode.type}@${fromNode.position.toString(16)}/'${fromSocket.name}', " +
                "to: ${toNode.type}@${fromNode.position.toString(16)}/'${toSocket.name}' }"
    }

}