package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("unused")
class BID(ptr: ConstructorData) : BlendData(ptr) {

    val next get() = getPointer("*next")
    val prev get() = getPointer("*prev")

    // val name = string("name[66]", 66)
    val typeName get() = string("name[66]", 2)!!
    val realName get() = string(getOffset("name[66]") + 2, 64)!!

    // tags, flags, ...
    override fun toString(): String {
        return "ID { $typeName, $realName }"
    }
}