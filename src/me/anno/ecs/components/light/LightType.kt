package me.anno.ecs.components.light

import me.anno.ecs.components.light.PointLight.Companion.falloff
import me.anno.ecs.components.light.PointLight.Companion.falloff2d
import me.anno.ecs.components.light.SpotLight.Companion.coneFunction

enum class LightType(val id: Int, val falloff: String, val shadowMapType: ShadowMapType) {
    DIRECTIONAL(0, "max(0.0, dir.z)", ShadowMapType.PLANE),
    SPOT(1, falloff, ShadowMapType.PLANE),
    POINT(2, falloff, ShadowMapType.CUBEMAP);
    // todo environment map is another type
    // todo combine it with shadows somehow? idk...
}