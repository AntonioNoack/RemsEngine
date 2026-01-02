package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BLink

@Suppress("unused", "SpellCheckingInspection")
class BNodeLink(ptr: ConstructorData) : BLink<BNodeLink>(ptr) {

    val fromNode = getPointer("*fromnode") as BNode
    val fromSocket = getPointer("*fromsock") as BNodeSocket
    val toNode = getPointer("*tonode") as BNode
    val toSocket = getPointer("*tosock") as BNodeSocket

    // flag, multi_input_socket_index

    override fun toString(): String {
        return "bNodeLink { " +
                "from: ${fromNode.type}@${fromNode.positionInFile.toString(16)}/'${fromSocket.name}', " +
                "to: ${toNode.type}@${fromNode.positionInFile.toString(16)}/'${toSocket.name}' }"
    }
}