package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class BMaterial(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val id = inside("id") as? BID

    val r = float("r")
    val g = float("g")
    val b = float("b")
    val a = float("a") // or alpha?

    val roughness = float("roughness")
    val metallic = float("metallic")

}