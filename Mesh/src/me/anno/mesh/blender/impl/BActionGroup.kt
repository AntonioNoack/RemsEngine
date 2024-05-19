package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

/**
 * https://github.com/blender/blender/blob/main/source/blender/makesdna/DNA_action_types.h#L628
 * */
class BActionGroup(ptr: ConstructorData) : BLink<BActionGroup>(ptr) {

    val name = string("name[64]", 64)

    override fun toString(): String {
        return "bActionGroup { '$name' }"
    }

}