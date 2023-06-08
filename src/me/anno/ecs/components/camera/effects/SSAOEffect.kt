package me.anno.ecs.components.camera.effects

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Range
import kotlin.math.max

class SSAOEffect {
    var strength = 1f
    var radius = 2f // 0.1 of world size looks pretty good :)
    var samples = max(1, DefaultConfig["gpu.ssao.samples", 128])
    var enable2x2Blur = true
}