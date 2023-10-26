package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * data carrier of a doubly linked list
 * https://github.com/Blender/blender/blob/main/source/blender/makesdna/DNA_listBase.h
 * */
@Suppress("unused")
class BLinkData(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<BLinkData>(file, type, buffer, position) {

    val data get() = getStructArray("*data")

}