package me.anno.gpu.buffer

import org.lwjgl.opengl.GL46C

enum class BufferUsage(val id: Int) {
    /**
     * modified once, used many times
     * */
    STATIC(GL46C.GL_STATIC_DRAW),
    /**
     * modified repeatedly, used many times
     * */
    DYNAMIC(GL46C.GL_DYNAMIC_DRAW),
    /**
     * modified once, used at most a few times
     * */
    STREAM(GL46C.GL_STREAM_DRAW)
}