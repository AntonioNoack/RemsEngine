package me.anno.gpu.shader

@Suppress("unused")
enum class GLSLType(val glslName: String, val id: Int, val components: Int) {

    // boolean vectors
    V1B("bool", 0, 1),
    V2B("bvec2", 1, 2),
    V3B("bvec3", 2, 3),
    V4B("bvec4", 3, 4),

    // integer vectors
    V1I("int", 4, 1),
    V2I("ivec2", 5, 2),
    V3I("ivec3", 6, 3),
    V4I("ivec4", 7, 4),

    // float vectors
    V1F("float", 8, 1),
    V2F("vec2", 9, 2),
    V3F("vec3", 10, 3),
    V4F("vec4", 11, 4),

    // matrices
    M2x2("mat2", 12, 4),
    M3x3("mat3", 13, 9),
    M4x3("mat4x3", 14, 12),
    M4x4("mat4", 15, 16),

    // samplers / textures
    S2D("sampler2D", 16, 1000),
    S2DShadow("sampler2DShadow", 17, 1000),
    S2DI("isampler2D", 18, 1000),
    S2DU("usampler2D", 19, 1000),
    S3D("sampler3D", 20, 1000),
    S2DMS("sampler2DMS", 21, 1000),
    SCube("samplerCube", 22, 1000),
    SCubeShadow("samplerCubeShadow", 23, 1000),
    S2DA("sampler2DArray", 24, 1000),
    S2DAShadow("sampler2DArrayShadow", 25, 1000),

    IMAGE1D("image1d", 26, 1000),
    IMAGE2D("image2d", 27, 1000),
    IMAGE3D("image3d", 28, 1000),
    IMAGE_CUBE("imageCube", 29, 1000),

    BUFFER("buffer", 30, 1000),
    ;

    val isNativeInt = id in 0..7
    val isSampler = glslName.startsWith("sampler")

    override fun toString(): String = glslName

    fun withoutShadow(): GLSLType {
        return when (this) {
            S2DShadow -> S2D
            S2DAShadow -> S2DA
            SCubeShadow -> SCube
            else -> this
        }
    }

    companion object {
        val floats = listOf(V1F, V2F, V3F, V4F)
        val integers = listOf(V1I, V2I, V3I, V4I)
        val booleans = listOf(V1B, V2B, V3B, V4B)
    }
}
