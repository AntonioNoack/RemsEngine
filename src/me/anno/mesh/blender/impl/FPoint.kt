package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_anim_types.h#L571
 * */
class FPoint(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {
    // vec[2]: float, flag: int, _pad[4]: char
}