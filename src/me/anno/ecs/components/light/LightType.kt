package me.anno.ecs.components.light

import me.anno.ecs.components.light.PointLight.Companion.falloff

enum class LightType(val id: Int, val falloff: String, val shadowMapType: ShadowMapType) {
    DIRECTIONAL(0, "max(0.0, dir.z)", ShadowMapType.PLANE),
    SPOT(1, falloff, ShadowMapType.PLANE),
    POINT(2, falloff, ShadowMapType.CUBEMAP);
    // environment map could be another type -> not really, as its result needs to be normalized, and replaced with the sky, if none is nearby
}