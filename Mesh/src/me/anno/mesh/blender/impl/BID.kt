package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("unused")
class BID(ptr: ConstructorData) : BlendData(ptr) {

    val next get() = getPointer("*next")
    val prev get() = getPointer("*prev")

    val typeName get() = string("name", 2)!!
    val realName get() = string(getOffset("name") + 2, 64)!!

    // tags, flags, ...
    override fun toString(): String {
        return "ID { $typeName, $realName }"
    }
}