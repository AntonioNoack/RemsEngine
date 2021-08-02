package me.anno.ecs.components.light

import me.anno.ecs.components.light.PointLight.Companion.falloff
import me.anno.ecs.components.light.SpotLight.Companion.coneFunction

enum class LightType(val id: Int, val falloff: String) {
    DIRECTIONAL(0, "max(0.0, dir.z)"),
    SPOT_LIGHT(1, "$coneFunction * $falloff"), // todo position is awkward / not working
    POINT_LIGHT(2, falloff);
    // todo environment map is another type
    // todo combine it with shadows somehow? idk...
}