package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class BRenderData(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val frsSec = short("frs_sec") // 30.0 -> fps :)
    val frsSecBase = float("frs_sec_base") // 1.0, or 1.001, so for 29.97 XD
    val frameLen = float("framelen") // 1.0?

    override fun toString(): String {
        return "RenderData { $frsSec, $frsSecBase, $frameLen }"
    }
}