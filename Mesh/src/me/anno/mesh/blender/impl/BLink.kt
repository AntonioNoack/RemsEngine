package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("unused")
open class BLink<Type>(ptr: ConstructorData) : BlendData(ptr) {
    @Suppress("UNCHECKED_CAST")
    val next get() = getPointer("*next") as? Type
    @Suppress("UNCHECKED_CAST")
    val prev get() = getPointer("*prev") as? Type
}