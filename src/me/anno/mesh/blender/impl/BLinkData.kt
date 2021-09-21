package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class BLinkData(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val prev get() = getPointer("*prev") as? BLinkData
    val next get() = getPointer("*next") as? BLinkData
    val data get() = getStructArray("*data")

}