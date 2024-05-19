package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

class BCustomDataExternal(ptr: ConstructorData) : BlendData(ptr) {
    val fileName get() = string("filename[1024]", 1024)
}