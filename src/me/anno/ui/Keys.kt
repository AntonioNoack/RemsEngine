package me.anno.ui

import org.lwjgl.glfw.GLFW

object Keys {

    @JvmStatic
    fun Int.isClickKey() = when(this){
        GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> true
        else -> false
    }

    @JvmStatic
    fun Int.isDownKey() = when(this){
        GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_DOWN -> true
        else -> false
    }

    @JvmStatic
    fun Int.isUpKey() = when(this){
        GLFW.GLFW_KEY_UP -> true
        else -> false
    }

}