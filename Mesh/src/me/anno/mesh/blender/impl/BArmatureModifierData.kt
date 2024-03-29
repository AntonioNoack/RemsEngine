package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 *
 * */
class BArmatureModifierData(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BModifierData(file, type, buffer, position) {

    /*
    * modifier: ModifierData, deformflag: short, multi: short,
    * *object: Object, (*vert_coords_prev)(): float, defgrp_name[64]: char
    * */

    val armatureObject = getPointer("*object") as? BObject

    override fun toString(): String {
        return "ArmatureModifierData { $armatureObject }"
    }
}