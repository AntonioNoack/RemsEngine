package me.anno.ecs.components.light

import me.anno.gpu.shader.GLSLType

enum class LightType(val id: Int, val shadowMapType: GLSLType) {
    DIRECTIONAL(0, GLSLType.S2DShadow),
    SPOT(1, GLSLType.S2DShadow),
    POINT(2, GLSLType.SCubeShadow);
    // environment map could be another type -> not really, as its result needs to be normalized, and replaced with the sky, if none is nearby
}