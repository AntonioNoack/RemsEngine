package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection")
class BImagePackedFile(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<BImagePackedFile>(file, type, buffer, position) {

    val filepath = string("filepath[1024]", 1024)
    val packedFile = getPointer("*packedfile") as? BPackedFile

    val view = int("view")
    val tileNumber = int("tile_number") // for UDIMs

    override fun toString(): String {
        return "ImagePackedFile { packed: $packedFile, path: $filepath, view: $view, tile#: $tileNumber }"
    }
}