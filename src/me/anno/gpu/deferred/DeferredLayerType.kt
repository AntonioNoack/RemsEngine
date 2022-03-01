package me.anno.gpu.deferred

import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r

enum class DeferredLayerType(
    val glslName: String,
    val dimensions: Int,
    val minimumQuality: BufferQuality,
    val highDynamicRange: Boolean,
    val defaultValueARGB: Int,
    val map01: String,
    val map10: String
) {

    COLOR("finalColor", 3, BufferQuality.LOW_8, false, 0x7799ff, "", ""),
    EMISSIVE("finalEmissive", 3, BufferQuality.MEDIUM_12, true, 0, "", ""), // could need high precision...

    // todo this is special, integrate it somehow...
    COLOR_EMISSIVE("finalColorEmissive", 4, BufferQuality.LOW_8, false, 0x007799ff, "", ""),

    // todo we should add randomness like with finalColor, maybe to all buffers for random rounding
    NORMAL("finalNormal", 3, BufferQuality.MEDIUM_12, false, 0x77ff77, "*0.5+0.5", "*2.0-1.0"),

    // todo do we need the tangent? it is calculated from uvs, so maybe for anisotropy...
    // high precision is required for curved metallic objects; otherwise we get banding
    TANGENT("finalTangent", 3, BufferQuality.MEDIUM_12, false, 0x7777ff, "*0.5+0.5", "*2.0-1.0"),

    // may be in camera space, player space, or world space
    // the best probably would be player space: relative to the player, same rotation, scale, etc as world
    POSITION("finalPosition", 3, BufferQuality.HIGH_32, true, 0, "", ""),

    METALLIC("finalMetallic", 0),
    ROUGHNESS("finalRoughness", 0x11), // roughness = 1-reflectivity
    OCCLUSION("finalOcclusion", 0xff), // from an occlusion texture, cavity; 1 = no cavities, 0 = completely hidden

    // transparency? is a little late... finalAlpha, needs to be handled differently
    TRANSLUCENCY("finalTranslucency", 0),
    SHEEN("finalSheen", 0),
    SHEEN_NORMAL("finalSheenNormal", 3, 0x77ff77),

    // clear coat roughness? how would we implement that?
    // color, amount; e.g. for cars
    CLEAR_COAT("finalClearCoat", 4, 0xff9900ff.toInt()),
    CLEAT_COAT_ROUGH_METALLIC("finalClearCoatRoughMetallic", 2, 0x00ff),

    // can be used for water droplets: they are a coating with their own normals
    CLEAR_COAT_NORMAL("finalClearCoatNormal", 3, 0x77ff77),

    // color + radius/intensity, e.g. for skin
    SUBSURFACE("finalSubsurface", 4, 0x00ffffff),

    // amount, rotation
    ANISOTROPIC("finalAnisotropic", 2, 0),

    // needs some kind of mapping...
    INDEX_OF_REFRACTION("finalIndexOfRefraction", 0),

    // ids / markers
    ID("finalId", 4, 0),
    FLAGS("finalFlags", 4, 0)

    // is there more, which we could use?

    ;

    constructor(glslName: String, defaultValueARGB: Int) :
            this(glslName, 1, BufferQuality.LOW_8, false, defaultValueARGB, "", "")

    constructor(glslName: String, dimensions: Int, defaultValueARGB: Int) :
            this(glslName, dimensions, BufferQuality.LOW_8, false, defaultValueARGB, "", "")

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
                if (defaultValueARGB.g() != defaultValueARGB.b()) {
                    fragment.append(", ")
                    fragment.append(defaultValueARGB.b() / 255f)
                }
                fragment.append(')')
            }
            3 -> {
                fragment.append("vec3(")
                fragment.append(defaultValueARGB.r() / 255f)
                if (defaultValueARGB.r() != defaultValueARGB.g() || defaultValueARGB.r() != defaultValueARGB.b()) {
                    fragment.append(", ")
                    fragment.append(defaultValueARGB.g() / 255f)
                    fragment.append(", ")
                    fragment.append(defaultValueARGB.b() / 255f)
                }
                fragment.append(')')
            }
            4 -> {
                fragment.append("vec4(")
                fragment.append(defaultValueARGB.a() / 255f)
                if (defaultValueARGB != defaultValueARGB.b() * 0x1010101) {
                    fragment.append(", ")
                    fragment.append(defaultValueARGB.r() / 255f)
                    fragment.append(", ")
                    fragment.append(defaultValueARGB.g() / 255f)
                    fragment.append(", ")
                    fragment.append(defaultValueARGB.b() / 255f)
                }
                fragment.append(')')
            }
        }
    }

    fun getValue(settingsV2: DeferredSettingsV2, uv: String = "uv"): String {
        val layer = settingsV2.layers.first { it.type == this }
        return "texture(${layer.textureName}, $uv).${layer.mapping}$map10"
    }

    companion object {
        val values2 = values()
    }

}
