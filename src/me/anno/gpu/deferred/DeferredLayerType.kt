package me.anno.gpu.deferred

// todo buffer index and channels can be computed at runtime per renderer
enum class DeferredLayerType(
    val glslName: String,
    val dimensions: Int,
    val needsHighPrecision: Boolean,
    val defaultValueARGB: Int
) {

    COLOR("finalColor", 3, false, -1),
    EMISSIVE("finalEmissive", 4, false, 0), // 4th component for additional exponent?

    NORMAL("finalNormal", 3, false, 0x77ff77),
    TANGENT("finalTangent", 3, false, 0x7777ff),

    // may be in camera space, player space, or world space
    // the best probably would be player space: relative to the player, same rotation, scale, etc as world
    POSITION("finalPosition", 3, true, 0),

    METALLIC("finalMetallic", 1, false, 0),
    ROUGHNESS("finalRoughness", 1, false, 0x11), // roughness = 1-reflectivity
    OCCLUSION("finalOcclusion", 1, false, 0), // from an occlusion texture, cavity

    // transparency? is a little late...


    // amount, roughness
    CLEAR_COAT("finalClearCoat", 2, false, 0),

    // can be used for water droplets: they are a coating with their own normals
    CLEAR_COAT_NORMAL("finalClearCoatNormal", 3, false, 0x77ff77),

    // amount, rotation
    ANISOTROPIC("finalAnisotropic", 2, false, 0),

    // needs some kind of mapping...
    INDEX_OF_REFRACTION("finalIndexOfRefraction", 1, false, 0),

    // ids / markers
    ID("finalId", 4, false, 0),
    FLAGS("finalFlags", 4, false, 0)

    // is there more, which we could use?

}

// todo an automated mapping of these properties
