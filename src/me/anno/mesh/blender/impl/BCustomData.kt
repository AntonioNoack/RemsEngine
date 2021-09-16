package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class BCustomData(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val external = ptr("*external")
    val size = int("totsize")
    val numLayers = int("totlayer")
    val maxLayer = int("maxlayer")
    val layers = ptr("*layers")

}