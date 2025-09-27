package me.anno.gpu.query

import org.lwjgl.opengl.GL46C.GL_SAMPLES_PASSED

class OcclusionQuery(var minSamples: Int = 16, everyNthFrame: Int = 16) :
    StackableGPUQuery(data, everyNthFrame) {

    companion object {
        // to do mode, where depth-prepass is used for occlusion queries instead of color pass,
        //  and color is only drawn, where the query is positive
        // is changed to GL_ANY_SAMPLES_PASSED_CONSERVATIVE, where it is available
        val data = StackableQueryData(GL_SAMPLES_PASSED)
        var target: Int
            get() = data.target
            set(value) {
                data.target = value
            }
    }

    val drawnSamples get() = result

    val wasVisible
        get(): Boolean {
            val result = drawnSamples
            return result !in 0 until minSamples
        }

    override fun toString(): String {
        return "OQ($drawnSamples)"
    }
}