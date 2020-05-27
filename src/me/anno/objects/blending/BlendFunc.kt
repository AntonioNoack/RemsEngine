package me.anno.objects.blending

import org.lwjgl.opengl.GL14.*

enum class BlendFunc(val mode: Int, val hasParams: Boolean){
    ADD(GL_FUNC_ADD, true),
    SUB(GL_FUNC_SUBTRACT, true),
    REV_SUB(GL_FUNC_REVERSE_SUBTRACT, true),
    MIN(GL_MIN, false),
    MAX(GL_MAX, false)
}