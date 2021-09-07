package me.anno.gpu.pipeline

import me.anno.ecs.Transform
import me.anno.ecs.components.light.LightComponent

class LightRequest<V : LightComponent>(var light: V, var transform: Transform) {
    fun set(l: V, t: Transform) {
        light = l
        transform = t
    }
}