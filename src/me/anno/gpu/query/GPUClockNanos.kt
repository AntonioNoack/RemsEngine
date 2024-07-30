package me.anno.gpu.query

import org.lwjgl.opengl.GL46C.GL_TIME_ELAPSED

class GPUClockNanos : StackableGPUQuery(data) {
    companion object {
        val data = StackableQueryData(GL_TIME_ELAPSED)
    }
}