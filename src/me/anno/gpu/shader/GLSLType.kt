package me.anno.gpu.shader

@Suppress("unused")
enum class GLSLType(val glslName: String, val id: Int, val components: Int, val isFlat: Boolean) {

    V1B("bool", 0, 1, true),
    V2B("bvec2", 1, 2, true),
    V3B("bvec3", 2, 3, true),
    V4B("bvec4", 3, 4, true),
    V1I("int", 4, 1, true),
    V2I("ivec2", 5, 2, true),
    V3I("ivec3", 6, 3, true),
    V4I("ivec4", 7, 4, true),
    V1F("float", 8, 1, false),
    V2F("vec2", 9, 2, false),
    V3F("vec3", 10, 3, false),
    V4F("vec4", 11, 4, false),
    M2x2("mat2", 12, 4, false),
    M3x3("mat3", 13, 9, false),
    M4x3("mat4x3", 14, 12, false),
    M4x4("mat4", 15, 16, false),
    S2D("sampler2D", 16, 1000, false),
    S2DShadow("sampler2DShadow", 17, 1000, false),
    S2DI("isampler2D", 18, 1000, false),
    S2DU("usampler2D", 19, 1000, false),
    S3D("sampler3D", 20, 1000, false),
    S2DMS("sampler2DMS", 21, 1000, false),
    SCube("samplerCube", 22, 1000, false),
    SCubeShadow("samplerCubeShadow", 23, 1000, false),
    S2DA("sampler2DArray", 24, 1000, false),
    S2DAShadow("sampler2DArrayShadow", 25, 1000, false),
    ;

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
