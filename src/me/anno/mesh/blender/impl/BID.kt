package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer
import kotlin.math.min

@Suppress("unused")
class BID(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val next get() = getPointer("*next")
    val prev get() = getPointer("*prev")

    val name = string("name[66]", 66)
    val typeName = name.substring(0, min(name.length, 2))
    val realName = name.substring(min(name.length, 2))

    // tags, flags, ...
    override fun toString(): String {
        return "ID { $typeName, $realName }"
    }
}