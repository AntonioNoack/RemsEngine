package me.anno.gpu.deferred

import me.anno.gpu.deferred.DeferredSettingsV2.Companion.glslTypes
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector4f

open class DeferredLayerType(
    val name: String,
    val glslName: String,
    val workDims: Int,
    val dataDims: Int,
    val minimumQuality: BufferQuality, // todo this depends on the platform; todo or we could use a mapping between attributes :)
    val highDynamicRange: Boolean,
    val defaultWorkValue: Vector4f,
    val workToData: String,
    val dataToWork: String
) {

    override fun toString() = name

    constructor(
        name: String, glslName: String, dimensions: Int, minimumQuality: BufferQuality, highDynamicRange: Boolean,
        defaultValueARGB: Int, w2d: String, d2w: String
    ) : this(
        name,
        glslName,
        dimensions, dimensions,
        minimumQuality, highDynamicRange,
        defaultValueARGB.toVecRGBA(),
        w2d, d2w
    )

    constructor(name: String, glslName: String, defaultValueARGB: Int) :
            this(name, glslName, 1, BufferQuality.LOW_8, false, defaultValueARGB, "", "")

    constructor(name: String, glslName: String, dimensions: Int, defaultValueARGB: Int) :
            this(name, glslName, dimensions, BufferQuality.LOW_8, false, defaultValueARGB, "", "")

    fun appendDefinition(fragment: StringBuilder) {
        fragment.append(glslTypes[workDims - 1])
        fragment.append(' ')
        fragment.append(glslName)
    }

    fun appendDefaultValue(fragment: StringBuilder) {
        when (workDims) {
            1 -> fragment.append(defaultWorkValue.z)
            2 -> fragment.append("vec2(")
                .append(defaultWorkValue.y)
                .append(", ")
                .append(defaultWorkValue.z)
                .append(')')
            3 -> fragment.append("vec3(")
                .append(defaultWorkValue.x)
                .append(", ")
                .append(defaultWorkValue.y)
                .append(", ")
                .append(defaultWorkValue.z)
                .append(')')
            4 -> fragment.append("vec4(")
                .append(defaultWorkValue.x)
                .append(", ")
                .append(defaultWorkValue.y)
                .append(", ")
                .append(defaultWorkValue.z)
                .append(", ")
                .append(defaultWorkValue.w)
                .append(')')
        }
    }

    companion object {

        val COLOR = DeferredLayerType(
            "Color", "finalColor", 3, BufferQuality.LOW_8,
            false, 0x7799ff, "", ""
        )

        // could need high precision...
        val EMISSIVE = DeferredLayerType(
            "Emissive", "finalEmissive", 3, BufferQuality.MEDIUM_12,
            true, 0, "", ""
        )

        /***
         * normal, encoded in 2d!, so please unpack and pack it correctly using the function in ShaderLib
         * */
        val NORMAL = DeferredLayerType(
            "Normal", "finalNormal",
            3, 2, BufferQuality.MEDIUM_12, false,
            0x77ff77.toVecRGBA(), "PackNormal", "UnpackNormal"
        )

        // todo do we need the tangent? it is calculated from uvs, so maybe for anisotropy...
        // high precision is required for curved metallic objects; otherwise we get banding
        val TANGENT = DeferredLayerType(
            "Tangent", "finalTangent",
            3, 2, BufferQuality.MEDIUM_12, false,
            0x7777ff.toVecRGBA(), "PackNormal", "UnpackNormal"
        )

        val BITANGENT = DeferredLayerType(
            "Bitangent", "finalBitangent",
            3, 2, BufferQuality.MEDIUM_12, false,
            0x7777ff.toVecRGBA(), "PackNormal", "UnpackNormal"
        )

        // may be in camera space, player space, or world space
        // the best probably would be player space: relative to the player, same rotation, scale, etc. as world
        val POSITION = DeferredLayerType("Position", "finalPosition", 3, BufferQuality.HIGH_32, true, 0, "", "")

        val METALLIC = DeferredLayerType("Metallic", "finalMetallic", 0)

        // roughness = 1-reflectivity
        val ROUGHNESS = DeferredLayerType("Roughness", "finalRoughness", 0x11)

        // from an occlusion texture, cavity; 1 = no cavities, 0 = completely hidden
        // textures in materials are typically inverted, so they can be inverted here as well
        val OCCLUSION = DeferredLayerType("Occlusion", "finalOcclusion", 0)

        // transparency? is a little late... finalAlpha, needs to be handled differently
        val TRANSLUCENCY = DeferredLayerType("Translucency", "finalTranslucency", 0)
        val SHEEN = DeferredLayerType("Sheen", "finalSheen", 0)
        val SHEEN_NORMAL = DeferredLayerType("Sheen Normal", "finalSheenNormal", 3, 0x77ff77)

        // clear coat roughness? how would we implement that?
        // color, amount; e.g. for cars
        val CLEAR_COAT = DeferredLayerType("Clear Coat", "finalClearCoat", 4, 0xff9900ff.toInt())
        val CLEAT_COAT_ROUGH_METALLIC = DeferredLayerType("Clear Coat Roughness + Metallic", "finalClearCoatRoughMetallic", 2, 0x00ff)

        // can be used for water droplets: they are a coating with their own normals
        val CLEAR_COAT_NORMAL = DeferredLayerType("Cleat Coat Normal", "finalClearCoatNormal", 3, 0x77ff77)

        // color + radius/intensity, e.g. for skin
        val SUBSURFACE = DeferredLayerType("Subsurface", "finalSubsurface", 4, 0x00ffffff)

        // amount, rotation
        val ANISOTROPIC = DeferredLayerType("Anisotropy", "finalAnisotropic", 2, 0)

        // needs some kind of mapping...
        val INDEX_OF_REFRACTION = DeferredLayerType("Index of Refraction", "finalIndexOfRefraction", 0)

        // ids / markers
        val ID = DeferredLayerType("ID", "finalId", 4, 0)
        val FLAGS = DeferredLayerType("Flags", "finalFlags", 4, 0)

        // pseudo types for effects
        val HDR_RESULT = DeferredLayerType("HDR", "finalHDR", 0)
        val SDR_RESULT = DeferredLayerType("SDR", "finalSDR", 0)
        val LIGHT_SUM = DeferredLayerType("Light Sum", "finalLight", 0)

        // is there more, which we could use?

        // todo this is special, integrate it somehow...
        val COLOR_EMISSIVE = DeferredLayerType(
            "Color,Emissive", "finalColorEmissive", 4,
            BufferQuality.LOW_8, false, 0x007799ff, "", ""
        )

        // use baked depth instead of this, this is kind of virtual
        val DEPTH = DeferredLayerType(
            "Depth", "finalDepth", 1,
            BufferQuality.HIGH_32, true, 0, "depthToRaw", "rawToDepth"
        )

        // make there should be an option for 2d motion vectors as well
        val MOTION = DeferredLayerType(
            "Motion Vectors", "finalMotion", 3,
            BufferQuality.HIGH_16, true, 0, "", ""
        )

        val ALPHA = DeferredLayerType(
            "Opacity", "finalAlpha", 1, BufferQuality.LOW_8,
            false, 0, "", ""
        )

        val values = arrayListOf(
            COLOR,
            ALPHA,
            EMISSIVE,
            NORMAL,
            TANGENT,
            POSITION,
            METALLIC,
            ROUGHNESS,
            TRANSLUCENCY,
            SHEEN,
            SHEEN_NORMAL,
            CLEAR_COAT,
            CLEAR_COAT_NORMAL,
            OCCLUSION,
            SUBSURFACE,
            ANISOTROPIC,
            INDEX_OF_REFRACTION,
            COLOR_EMISSIVE,
            MOTION,
            DEPTH
        )

        // stencil?

        val byName = LazyMap { name: String ->
            // O(n), but should be called only for new types (which are rare)
            values.firstOrNull { it.glslName == name }
        }.putAll(values.associateBy { it.glslName }) // avoid O(n²)

    }

}
