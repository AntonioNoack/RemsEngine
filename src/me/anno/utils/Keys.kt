package me.anno.utils

import org.lwjgl.glfw.GLFW

object Keys {

    fun Int.isClickKey() = when(this){
        GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> true
        else -> false
    }

    fun Int.isDownKey() = when(this){
        GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_DOWN -> true
        else -> false
    }

    fun Int.isUpKey() = when(this){
        GLFW.GLFW_KEY_UP -> true
        else -> false
    }

}