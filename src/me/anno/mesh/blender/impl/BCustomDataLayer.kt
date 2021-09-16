package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

class BCustomDataLayer(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val typeInt = int("type")
    val offset = int("offset")
    val active = int("active")
    val uid = int("uid")
    val name = string("name[64]", 64)
    val data = ptr("*data") // void

}