package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("SpellCheckingInspection")
class BActionChannel(ptr: ConstructorData) : BLink<BActionChannel>(ptr) {

    val name = string("name[64]", 64)

    // *next: bActionChannel, *prev: bActionChannel, *grp: bActionGroup, *ipo: Ipo, constraintChannels: ListBase, flag: int, name[64]: char, temp: int

    override fun toString(): String {
        return "bActionChannel { '$name' }"
    }

}