package me.anno.gpu.pipeline

import me.anno.ecs.Transform

/**
 * container for instanced transforms and their click ids
 * */
open class InstancedStack {

    var transforms = arrayOfNulls<Transform>(16)
    var clickIds = IntArray(16)
    var size = 0

    fun clear() {
        size = 0
    }

    fun isNotEmpty() = size > 0
    fun isEmpty() = size == 0

    open fun add(transform: Transform, clickId: Int) {
        if (size >= transforms.size) {
            // resize
            val newSize = transforms.size * 2
            val newTransforms = arrayOfNulls<Transform>(newSize)
            val newClickIds = IntArray(newSize)
            System.arraycopy(transforms, 0, newTransforms, 0, size)
            System.arraycopy(clickIds, 0, newClickIds, 0, size)
            transforms = newTransforms
            clickIds = newClickIds
        }
        val index = size++
        transforms[index] = transform
        clickIds[index] = clickId
    }

}