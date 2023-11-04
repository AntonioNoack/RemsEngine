package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("unused")
class BCustomDataLayer(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val type = int("type")
    val offset = int("offset")
    val active = int("active")
    val uid = int("uid")
    val name get() = string("name[64]", 64) ?: string("name[68]", 68) // newer versions have slightly more budget
    val data = getInstantList<BlendData>("*data") ?: BInstantList.emptyList()

    override fun toString(): String {
        return "BCustomDataLayer { '$name', type: ${data.instance?.javaClass?.name}, data: ${data.size}x, sample: ${data.firstOrNull()} }"
    }
}