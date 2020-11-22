package me.anno.gpu.texture

import org.lwjgl.opengl.GL14.*

enum class Clamping(val id: Int, val displayName: String, val mode: Int){
    CLAMP(0, "Clamp", GL_CLAMP_TO_EDGE),
    REPEAT(1, "Repeat", GL_REPEAT),
    MIRRORED_REPEAT(2, "Mirrored", GL_MIRRORED_REPEAT)
}