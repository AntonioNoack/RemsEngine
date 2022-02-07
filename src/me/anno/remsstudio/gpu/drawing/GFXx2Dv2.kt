package me.anno.remsstudio.gpu.drawing

import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.shader.Shader
import me.anno.remsstudio.objects.GFXTransform
import me.anno.remsstudio.objects.Transform
import me.anno.remsstudio.objects.Video

object GFXx2Dv2 {

    fun defineAdvancedGraphicalFeatures(shader: Shader, transform: Transform?, time: Double) {
        (transform as? GFXTransform)?.uploadAttractors(shader, time) ?: GFXx3D.uploadAttractors0(shader)
        GFXx3Dv2.colorGradingUniforms(transform as? Video, time, shader)
    }

}