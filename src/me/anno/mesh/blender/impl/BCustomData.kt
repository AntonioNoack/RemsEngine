package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection", "unused")
class BCustomData(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val external = getStructArray("*external")
    val size = int("totsize")
    val numLayers = int("totlayer")
    val maxLayer = int("maxlayer")
    val layers = getStructArray("*layers")

}