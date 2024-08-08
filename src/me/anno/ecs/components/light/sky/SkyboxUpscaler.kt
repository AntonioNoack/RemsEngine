package me.anno.ecs.components.light.sky

import me.anno.ecs.components.light.sky.shaders.SkyUpscaleShader

object SkyboxUpscaler : SkyboxBase() {
    init {
        material.shader = SkyUpscaleShader
    }
}