package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection", "unused", "UNCHECKED_CAST")
class BCustomData(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val external get() = getStructArray("*external")
    val size = int("totsize")
    val numLayers = int("totlayer")
    val maxLayer = int("maxlayer")
    val layers get() = getStructArray("*layers")?.toList() as? List<BCustomDataLayer> ?: emptyList()

    override fun toString(): String {
        return "BCustomData { layers: ${layers}, external: $external }"
    }

}