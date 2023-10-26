package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
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
        return "BNodeLink { from: [$fromNode, $fromSocket], to: [$toNode, $toSocket] }"
    }

}