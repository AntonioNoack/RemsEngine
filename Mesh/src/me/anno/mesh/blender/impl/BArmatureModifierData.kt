package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

class BArmatureModifierData(ptr: ConstructorData) : BModifierData(ptr) {

    /*
    * modifier: ModifierData, deformflag: short, multi: short,
    * *object: Object, (*vert_coords_prev)(): float, defgrp_name[64]: char
    * */

    val armatureObject = getPointer("*object") as? BObject

    override fun toString(): String {
        return "ArmatureModifierData { $armatureObject }"
    }
}