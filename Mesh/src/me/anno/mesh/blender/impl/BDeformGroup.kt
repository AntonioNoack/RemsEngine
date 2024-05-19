package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

class BDeformGroup(ptr: ConstructorData) : BLink<BDeformGroup>(ptr) {

    // { *next: bDeformGroup, *prev: bDeformGroup, name[64]: char, flag: char, _pad0[7]: char }

    val name = string("name[64]", 64)

    override fun toString(): String {
        return "bDeformGroup { '$name' }"
    }

}