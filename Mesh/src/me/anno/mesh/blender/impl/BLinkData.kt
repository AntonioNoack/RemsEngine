package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

/**
 * data carrier of a doubly linked list
 * https://github.com/Blender/blender/blob/main/source/blender/makesdna/DNA_listBase.h
 * */
@Suppress("unused")
class BLinkData(ptr: ConstructorData) : BLink<BLinkData>(ptr) {
    val data get() = getStructArray<BlendData>("*data")
}