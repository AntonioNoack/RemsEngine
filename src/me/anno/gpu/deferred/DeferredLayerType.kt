package me.anno.gpu.deferred

import me.anno.utils.Color.toVecRGBA
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector4fc

open class DeferredLayerType(
    val name: String,
    val glslName: String,
    val dimensions: Int,
    val minimumQuality: BufferQuality, // todo this depends on the platform; todo or we could use a mapping between attributes :)
    val highDynamicRange: Boolean,
    val defaultValueARGB: Vector4fc,
    val map01: String,
    val map10: String
) {

    constructor(
        name: String, glslName: String, dimensions: Int, minimumQuality: BufferQuality, highDynamicRange: Boolean,
        defaultValueARGB: Int, map01: String, map10: String
    ) : this(name, glslName, dimensions, minimumQuality, highDynamicRange, defaultValueARGB.toVecRGBA(), map01, map10)

    constructor(name: String, glslName: String, defaultValueARGB: Int) :
            this(name, glslName, 1, BufferQuality.LOW_8, false, defaultValueARGB, "", "")

    constructor(name: String, glslName: String, dimensions: Int, defaultValueARGB: Int) :
            this(name, glslName, dimensions, BufferQuality.LOW_8, false, defaultValueARGB, "", "")

    fun appendDefinition(fragment: StringBuilder) {
        fragment.append(DeferredSettingsV2.glslTypes[dimensions - 1])
        fragment.append(' ')
        fragment.append(glslName)
    }

    fun appendDefaultValue(fragment: StringBuilder) {
        when (dimensions) {
            1 -> {
                fragment.append(defaultValueARGB.x())
            }
            2 -> {
                fragment.append("vec2(")
                fragment.append(defaultValueARGB.x())
                fragment.append(", ")
                fragment.append(defaultValueARGB.y())
                fragment.append(')')
            }
            3 -> {
                fragment.append("vec3(")
                fragment.append(defaultValueARGB.x())
                fragment.append(", ")
                fragment.append(defaultValueARGB.y())
                fragment.append(", ")
                fragment.append(defaultValueARGB.z())
                fragment.append(')')
            }
            4 -> {
                fragment.append("vec4(")
                fragment.append(defaultValueARGB.x())
                fragment.append(", ")
                fragment.append(defaultValueARGB.y())
                fragment.append(", ")
                fragment.append(defaultValueARGB.z())
                fragment.append(", ")
                fragment.append(defaultValueARGB.w())
                fragment.append(')')
            }
        }
    }

    fun getValue(settingsV2: DeferredSettingsV2, uv: String = "uv"): String {
        val layer = settingsV2.layers.first { it.type == this }
        return "texture(${layer.textureName}, $uv).${layer.mapping}$map10"
    }

    companion object {

        val COLOR = DeferredLayerType(
            "COLOR", "finalColor", 3, BufferQuality.LOW_8,
            false, 0x7799ff, "", ""
        )

        // could need high precision...
        val EMISSIVE = DeferredLayerType(
            "EMISSIVE", "finalEmissive", 3, BufferQuality.MEDIUM_12,
            true, 0, "", ""
        )

        // todo we should add randomness like with finalColor, maybe to all buffers for random rounding
        val NORMAL = DeferredLayerType(
            "NORMAL", "finalNormal", 3, BufferQuality.MEDIUM_12,
            false, 0x77ff77, "*0.5+0.5", "*2.0-1.0"
        )

        // todo do we need the tangent? it is calculated from uvs, so maybe for anisotropy...
        // high precision is required for curved metallic objects; otherwise we get banding
        val TANGENT = DeferredLayerType(
            "TANGENT",
            "finalTangent",
            3,
            BufferQuality.MEDIUM_12,
            false,
            0x7777ff,
            "*0.5+0.5",
            "*2.0-1.0"
        )

        // may be in camera space, player space, or world space
        // the best probably would be player space: relative to the player, same rotation, scale, etc as world
        val POSITION = DeferredLayerType("POSITION", "finalPosition", 3, BufferQuality.HIGH_32, true, 0, "", "")

        val METALLIC = DeferredLayerType("METALLIC", "finalMetallic", 0)

        // roughness = 1-reflectivity
        val ROUGHNESS = DeferredLayerType("ROUGHNESS", "finalRoughness", 0x11)

        // from an occlusion texture, cavity; 0 = no cavities, 1 = completely hidden
        // textures in materials are typically inverted, so they can be inverted here as well
        val OCCLUSION = DeferredLayerType("OCCLUSION", "finalOcclusion", 0)

        // transparency? is a little late... finalAlpha, needs to be handled differently
        val TRANSLUCENCY = DeferredLayerType("TRANSLUCENCY", "finalTranslucency", 0)
        val SHEEN = DeferredLayerType("SHEEN", "finalSheen", 0)
        val SHEEN_NORMAL = DeferredLayerType("SHEEN_NORMAL", "finalSheenNormal", 3, 0x77ff77)

        // clear coat roughness? how would we implement that?
        // color, amount; e.g. for cars
        val CLEAR_COAT = DeferredLayerType("CLEAR_COAT", "finalClearCoat", 4, 0xff9900ff.toInt())
        val CLEAT_COAT_ROUGH_METALLIC = DeferredLayerType("CLEAR_COAT_RM", "finalClearCoatRoughMetallic", 2, 0x00ff)

        // can be used for water droplets: they are a coating with their own normals
        val CLEAR_COAT_NORMAL = DeferredLayerType("CLEAR_COAT_NORMAL", "finalClearCoatNormal", 3, 0x77ff77)

        // color + radius/intensity, e.g. for skin
        val SUBSURFACE = DeferredLayerType("SUBSURFACE", "finalSubsurface", 4, 0x00ffffff)

        // amount, rotation
        val ANISOTROPIC = DeferredLayerType("ANISIOTROPIC", "finalAnisotropic", 2, 0)

        // needs some kind of mapping...
        val INDEX_OF_REFRACTION = DeferredLayerType("IOR", "finalIndexOfRefraction", 0)

        // ids / markers
        val ID = DeferredLayerType("ID", "finalId", 4, 0)
        val FLAGS = DeferredLayerType("FLAGS", "finalFlags", 4, 0)

        // pseudo types for effects
        val HDR_RESULT = DeferredLayerType("HDR", "finalHDR", 0)
        val SDR_RESULT = DeferredLayerType("SDR", "finalSDR", 0)
        val LIGHT_SUM = DeferredLayerType("LIGHT_SUM", "finalLight", 0)

        // is there more, which we could use?

        // todo this is special, integrate it somehow...
        val COLOR_EMISSIVE = DeferredLayerType(
            "COLOR_EMISSIVE", "finalColorEmissive", 4,
            BufferQuality.LOW_8, false, 0x007799ff, "", ""
        )

        // todo should be replaced by depth to save bandwidth
        val DEPTH = DeferredLayerType(
            "DEPTH", "finalDepth", 1,
            BufferQuality.HIGH_32, true, 0, "", ""
        )

        val MOTION = DeferredLayerType(
            "MOTION", "finalMotion", 2,
            BufferQuality.HIGH_16, true, 0, "", ""
        )

        val values = arrayListOf(
            COLOR,
            EMISSIVE,
            COLOR_EMISSIVE,
            NORMAL,
            TANGENT,
            POSITION,
            METALLIC,
            ROUGHNESS,
            OCCLUSION,
            TRANSLUCENCY,
            SHEEN,
            SHEEN_NORMAL,
            CLEAR_COAT,
            CLEAR_COAT_NORMAL,
            SUBSURFACE,
            ANISOTROPIC,
            INDEX_OF_REFRACTION,
            ID,
            FLAGS,
            HDR_RESULT,
            SDR_RESULT,
            LIGHT_SUM,
            COLOR_EMISSIVE,
            MOTION,
            DEPTH
        )

        val byName = LazyMap({ name: String ->
            // O(n), but should be called only for new types (which are rare)
            values.firstOrNull { it.glslName == name }
        }).putAll(values.associateBy { it.glslName }) // avoid O(n²)

    }

}
