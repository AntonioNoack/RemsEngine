package me.anno.gpu.query

import org.lwjgl.opengl.GL15C.*

class OcclusionQuery(var minSamples: Int = 16, everyNthFrame: Int = 4) : GPUQuery(Companion.target, everyNthFrame) {

    companion object {
        // to do mode, where depth-prepass is used for occlusion queries instead of color pass,
        //  and color is only drawn, where the query is positive
        // is changed to GL_ANY_SAMPLES_PASSED_CONSERVATIVE, where it is available
        var target = GL_SAMPLES_PASSED
    }

    val drawnSamples get() = result

    val wasVisible
        get(): Boolean {
            val result = drawnSamples
            return result < 0 || result >= minSamples
        }

}