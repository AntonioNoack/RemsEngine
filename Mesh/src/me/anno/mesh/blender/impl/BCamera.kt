package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("SpellCheckingInspection", "unused")
class BCamera(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val id = inside("id") as? BID

    val near = float("clipsta")
    val far = float("clipend")
    val orthoScale = float("ortho_scale")

}