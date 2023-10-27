package me.anno.mesh.blender.impl.nodes

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.mesh.blender.impl.BlendData
import java.nio.ByteBuffer

open class BTexMapping(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val obj = getPointer("*ob")

    // loc[3]=float(4)@0, rot[3]=float(4)@12, size[3]=float(4)@24, flag=int(4)@36,
    // projx=char(1)@40, projy=char(1)@41, projz=char(1)@42, mapping=char(1)@43, type=int(4)@44,
    // mat[4][4]=float(4)@48, min[3]=float(4)@112, max[3]=float(4)@124, *ob=Object(1488)@136

    override fun toString(): String {
        return "TexMapping { $obj }"
    }
}