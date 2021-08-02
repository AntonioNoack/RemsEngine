package me.anno.gpu.deferred

import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r

enum class DeferredLayerType(
    val glslName: String,
    val dimensions: Int,
    val needsHighPrecision: Boolean,
    val defaultValueARGB: Int,
    val map01: String,
    val map10: String
) {

    COLOR("finalColor", 3, false, 0xffffff, "", ""),
    EMISSIVE("finalEmissive", 3, false, 0, "", ""),

    // todo 12 bits per component? or sth like that?
    NORMAL("finalNormal", 3, false, 0x77ff77, "*0.5+0.5", "*2.0-1.0"),
    // todo do we need the tangent? it is calculated from uvs, so maybe for anisotropy...
    TANGENT("finalTangent", 3, false, 0x7777ff, "*0.5+0.5", "*2.0-1.0"),

    // may be in camera space, player space, or world space
    // the best probably would be player space: relative to the player, same rotation, scale, etc as world
    POSITION("finalPosition", 3, true, 0, "", ""),

    METALLIC("finalMetallic", 1, false, 0xff, "", ""),
    ROUGHNESS("finalRoughness", 1, false, 0x11, "", ""), // roughness = 1-reflectivity
    OCCLUSION("finalOcclusion", 1, false, 0, "", ""), // from an occlusion texture, cavity

    // transparency? is a little late... finalAlpha, needs to be handled differently

    // amount, roughness, e.g. for cars
    CLEAR_COAT("finalClearCoat", 2, false, 0, "", ""),

    // can be used for water droplets: they are a coating with their own normals
    CLEAR_COAT_NORMAL("finalClearCoatNormal", 3, false, 0x77ff77, "", ""),

    // color + radius/intensity, e.g. for skin
    SUBSURFACE("finalSubsurface", 4, false, 0x00ffffff, "", ""),

    // amount, rotation
    ANISOTROPIC("finalAnisotropic", 2, false, 0, "", ""),

    // needs some kind of mapping...
    INDEX_OF_REFRACTION("finalIndexOfRefraction", 1, false, 0, "", ""),

    // ids / markers
    ID("finalId", 4, false, 0, "", ""),
    FLAGS("finalFlags", 4, false, 0, "", "")

    // is there more, which we could use?

    ;

    fun appendDefinition(fragment: StringBuilder) {
        fragment.append(DeferredSettingsV2.glslTypes[dimensions - 1])
        fragment.append(' ')
        fragment.append(glslName)
    }

    fun appendDefaultValue(fragment: StringBuilder) {
        when (dimensions) {
            1 -> {
                fragment.append(defaultValueARGB.b() / 255f)
            }
            2 -> {
                fragment.append("vec2(")
                fragment.append(defaultValueARGB.g() / 255f)
                fragment.append(", ")
                fragment.append(defaultValueARGB.b() / 255f)
                fragment.append(')')
            }
            3 -> {
                fragment.append("vec3(")
                fragment.append(defaultValueARGB.r() / 255f)
                fragment.append(", ")
                fragment.append(defaultValueARGB.g() / 255f)
                fragment.append(", ")
                fragment.append(defaultValueARGB.b() / 255f)
                fragment.append(')')
            }
            4 -> {
                fragment.append("vec4(")
                fragment.append(defaultValueARGB.a() / 255f)
                fragment.append(", ")
                fragment.append(defaultValueARGB.r() / 255f)
                fragment.append(", ")
                fragment.append(defaultValueARGB.g() / 255f)
                fragment.append(", ")
                fragment.append(defaultValueARGB.b() / 255f)
                fragment.append(')')
            }
        }
    }

    fun getValue(settingsV2: DeferredSettingsV2,  uv: String = "uv"): String {
        val layer = settingsV2.layers.first { it.type == this }
        return "texture(${layer.textureName}, $uv).${layer.mapping}$map10"
    }

}
