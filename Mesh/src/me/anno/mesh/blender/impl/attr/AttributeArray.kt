package me.anno.mesh.blender.impl.attr

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

class AttributeArray(ptr: ConstructorData) : BlendData(ptr) {

    // [void(0) *data, ImplicitSharingInfoHandle(0) *sharing_info, int64_t(8) size

    val size get() = i64("size")
    val data get() = getPointer("*data") // raw_data for me... so we should just read it...
    val dataPtr get() = pointer(getOffset("*data")) // raw_data for me... so we should just read it...

    override fun toString(): String = "AttributeArray(size=$size)$data"
}