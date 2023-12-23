package me.anno.gfx

import me.anno.gpu.framebuffer.TargetType

interface DataBuffer {
    fun destroy()
}

class BufferSlice(val buffer: DataBuffer, val stride: Int, val offset: Int, val format: TargetType)