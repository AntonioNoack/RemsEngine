package me.anno.gpu.texture

import org.lwjgl.opengl.GL11.*

enum class NearestMode(val mag: Int, val min: Int){
    NEAREST(GL_NEAREST, GL_LINEAR_MIPMAP_LINEAR),
    TRULY_NEAREST(GL_NEAREST, GL_NEAREST),
    LINEAR(GL_LINEAR, GL_LINEAR_MIPMAP_LINEAR)
}