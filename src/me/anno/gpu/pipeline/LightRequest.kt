package me.anno.gpu.pipeline

import me.anno.ecs.Transform
import me.anno.ecs.components.light.LightComponent

class LightRequest(var light: LightComponent, var transform: Transform) {
    fun set(l: LightComponent, t: Transform) {
        light = l
        transform = t
    }
}