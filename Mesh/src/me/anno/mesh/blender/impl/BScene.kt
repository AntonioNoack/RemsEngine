package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("unused")
class BScene(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val id = inside("id") as BID
    // val nodeTree = getStructArray("*nodetree")
    val renderData = inside("r") as BRenderData

    override fun toString(): String {
        return "Scene { $id, $renderData }"
    }

}