package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.mesh.blender.impl.BlendData
import java.nio.ByteBuffer

open class BNodeTexBase(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BTexMapping(file, type, buffer, position) {

    // val texMapping = inside("tex_mapping") as BTexMapping
    // val colorMapping = inside("color_mapping") as BColorMapping

    override fun toString(): String {
        return "NodeTexBase { ${super.toString()} }"
    }
}