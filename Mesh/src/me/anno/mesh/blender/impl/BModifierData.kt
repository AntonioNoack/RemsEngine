package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

open class BModifierData(ptr: ConstructorData) :
    BLink<BModifierData>(ptr) {

    // val name = string("name[64]", 64)

    // { *next: ModifierData, *prev: ModifierData, type: int, mode: int, execution_time: float, flag: short,
    // ui_expand_flag: short, name[64]: char, *error: char, session_uuid: SessionUUID, *runtime: void }

}