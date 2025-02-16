package me.anno.gpu.pipeline

import me.anno.ecs.components.light.LightComponent
import org.joml.Matrix4x3f
import org.joml.Matrix4x3

class LightRequest(var light: LightComponent, var drawMatrix: Matrix4x3, var invCamSpaceMatrix: Matrix4x3f) {
    fun set(l: LightComponent, t: Matrix4x3, i: Matrix4x3f) {
        light = l
        drawMatrix = t
        invCamSpaceMatrix = i
    }
}