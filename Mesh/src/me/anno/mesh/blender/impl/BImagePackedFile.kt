package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("SpellCheckingInspection")
class BImagePackedFile(ptr: ConstructorData) : BLink<BImagePackedFile>(ptr) {

    val filepath = string("filepath[1024]", 1024)
    val packedFile = getPointer("*packedfile") as? BPackedFile

    val view = i32("view")
    val tileNumber = i32("tile_number") // for UDIMs

    override fun toString(): String {
        return "ImagePackedFile { packed: $packedFile, path: $filepath, view: $view, tile#: $tileNumber }"
    }
}