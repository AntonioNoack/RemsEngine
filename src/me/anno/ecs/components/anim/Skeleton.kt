package me.anno.ecs.components.anim

import org.lwjgl.BufferUtils

class Skeleton(val joints: Array<Joint>) {

    class Joint {

    }

    companion object {
        val gpuBuffer = BufferUtils.createFloatBuffer(12 * 256)
    }

}