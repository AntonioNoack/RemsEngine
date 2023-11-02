package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

open class BModifierData(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<BModifierData>(file, type, buffer, position) {

    // val name = string("name[64]", 64)

    // { *next: ModifierData, *prev: ModifierData, type: int, mode: int, execution_time: float, flag: short,
    // ui_expand_flag: short, name[64]: char, *error: char, session_uuid: SessionUUID, *runtime: void }

}