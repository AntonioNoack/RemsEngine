package me.anno.gpu.deferred

import me.anno.gpu.GFX
import me.anno.gpu.shader.GLSLType
import me.anno.utils.Color.black4
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector4f

class DeferredLayerType(
    val name: String,
    val glslName: String,
    val workDims: Int,
    val dataDims: Int,
    // todo this depends on the platform
    //  and it might be very useful to reduce some attributes to a few bits only, like roughness/metallic...
    val minimumQuality: BufferQuality,
    val highDynamicRange: Boolean,
    val defaultWorkValue: Vector4f,
    val workToData: String,
    val dataToWork: String,
) {

    override fun toString() = name

    constructor(
        name: String, glslName: String, dimensions: Int,
        minimumQuality: BufferQuality, highDynamicRange: Boolean,
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
            this(name, glslName, 1, BufferQuality.UINT_8, false, defaultValueARGB, "", "")

    constructor(name: String, glslName: String, dimensions: Int, defaultValueARGB: Int) :
            this(name, glslName, dimensions, BufferQuality.UINT_8, false, defaultValueARGB, "", "")

    fun appendDefinition(fragment: StringBuilder) {
        fragment.append(GLSLType.floats[workDims - 1])
        fragment.append(' ')
        fragment.append(glslName)
    }

    fun appendDefaultValue(fragment: StringBuilder, workDims: Int = this.workDims) {
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
            "Color", "finalColor", 3, BufferQuality.UINT_8,
            false, 0x7799ff, "", ""
        )

        // could need high precision...
        val EMISSIVE = DeferredLayerType(
            "Emissive", "finalEmissive", 3, BufferQuality.FP_16,
            true, 0, "", ""
        )

        /***
         * normal, encoded in 2d!, so please unpack and pack it correctly using the function in ShaderLib
         * */
        val NORMAL = DeferredLayerType(
            "Normal", "finalNormal",
            3, 2, BufferQuality.UINT_16, false,
            0x77ff77.toVecRGBA(), "PackNormal", "UnpackNormal"
        )

        // high precision is required for curved metallic objects; otherwise we get banding
        val TANGENT = DeferredLayerType(
            "Tangent", "finalTangent",
            3, 2, BufferQuality.UINT_16, false,
            0x7777ff.toVecRGBA(), "PackNormal", "UnpackNormal"
        )

        val BITANGENT = DeferredLayerType(
            "Bitangent", "finalBitangent",
            3, 2, BufferQuality.UINT_16, false,
            0x7777ff.toVecRGBA(), "PackNormal", "UnpackNormal"
        )

        // may be in camera space, player space, or world space
        // the best probably would be player space: relative to the player, same rotation, scale, etc. as world
        val POSITION = DeferredLayerType(
            "Position", "finalPosition", 3,
            BufferQuality.FP_32, true, 0, "", ""
        )

        // todo metallic and roughness can be joined into reflectivity
        val METALLIC = DeferredLayerType("Metallic", "finalMetallic", 0)
        val ROUGHNESS = DeferredLayerType("Roughness", "finalRoughness", 0xff)
        val REFLECTIVITY = DeferredLayerType(
            "Reflectivity", "finalReflectivity",
            1, 1, BufferQuality.UINT_8, false,
            black4, "", ""
        )

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
        val CLEAT_COAT_ROUGH_METALLIC =
            DeferredLayerType("Clear Coat Roughness + Metallic", "finalClearCoatRoughMetallic", 2, 0x00ff)

        // can be used for water droplets: they are a coating with their own normals
        val CLEAR_COAT_NORMAL = DeferredLayerType("Cleat Coat Normal", "finalClearCoatNormal", 3, 0x77ff77)

        // color + radius/intensity, e.g. for skin
        val SUBSURFACE = DeferredLayerType("Subsurface", "finalSubsurface", 4, 0x00ffffff)

        // amount, rotation
        val ANISOTROPIC = DeferredLayerType("Anisotropy", "finalAnisotropic", 2, 0)

        // needs some kind of mapping...
        val INDEX_OF_REFRACTION = DeferredLayerType("Index of Refraction", "finalIndexOfRefraction", 0)

        // ids / markers
        val CLICK_ID =
            DeferredLayerType("ClickID", "clickId", 3, BufferQuality.UINT_8, false, 0, "finalId.xyz", "finalId.xyz=")
        val GROUP_ID =
            DeferredLayerType("GroupID", "groupId", 1, BufferQuality.UINT_8, false, 0, "finalId.w", "finalId.w=")
        // val FLAGS = DeferredLayerType("Flags", "finalFlags", 4, 0)

        val LIGHT_SUM = DeferredLayerType("Light Sum", "finalLight", 3, 0)

        // is there more, which we could use?

        // todo this is special, integrate&define it somehow...
        val COLOR_EMISSIVE = DeferredLayerType(
            "Color,Emissive", "finalColorEmissive", 4,
            BufferQuality.UINT_8, false, 0x007799ff, "", ""
        )

        // use baked depth instead of this, this is kind of virtual
        val DEPTH = DeferredLayerType(
            "Depth", "finalDepth", 1,
            if (GFX.supportsClipControl) BufferQuality.FP_32
            else BufferQuality.DEPTH_U32, true, 0, "depthToRaw", "rawToDepth"
        )

        // there should be an option for 2d motion vectors as well
        val MOTION = DeferredLayerType(
            "Motion", "finalMotion", 3,
            BufferQuality.FP_16, true, 0, "", ""
        )

        val ALPHA = DeferredLayerType(
            "Alpha", "finalAlpha", 1, BufferQuality.UINT_8,
            false, 0xff, "", ""
        )

        val values = arrayListOf(
            COLOR,
            EMISSIVE,
            NORMAL,
            TANGENT,
            POSITION,
            METALLIC,
            ROUGHNESS,
            REFLECTIVITY,
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
            DEPTH,
            ALPHA,
            CLICK_ID,
            GROUP_ID,
        )

        // stencil?

        val byName = LazyMap { name: String ->
            // O(n), but should be called only for new types (which are rare)
            values.firstOrNull { it.glslName == name }
        }.putAll(values.associateBy { it.glslName }) // avoid O(n²)
    }
}
