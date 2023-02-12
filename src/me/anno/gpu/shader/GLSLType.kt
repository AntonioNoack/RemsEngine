package me.anno.gpu.shader

enum class GLSLType(val glslName: String, val components: Int, val isFlat: Boolean) {

    V1B("bool", 1, true),
    V2B("bvec2", 2, true),
    V3B("bvec3", 3, true),
    V4B("bvec4", 4, true),
    V1I("int", 1, true),
    V2I("ivec2", 2, true),
    V3I("ivec3", 3, true),
    V4I("ivec4", 4, true),
    V1F("float", 1, false),
    V2F("vec2", 2, false),
    V3F("vec3", 3, false),
    V4F("vec4", 4, false),
    M2x2("mat2", 4, false),
    M3x3("mat3", 9, false),
    M4x3("mat4x3", 12, false),
    M4x4("mat4", 16, false),
    S2D("sampler2D", 1000, false),
    S2DI("isampler2D", 1000, false),
    S2DU("usampler2D", 1000, false),
    S3D("sampler3D", 1000, false),
    S2DMS("sampler2DMS", 1000, false),
    SCube("samplerCube", 1000, false),
    S2DA("sampler2DArray", 1000, false),
    // S3DA("sampler3DArray", 1000, false),// not necessarily supported
    ;

    override fun toString(): String = glslName

    companion object {
        val floats = arrayOf(V1F, V2F, V3F, V4F)
        val integers = arrayOf(V1I, V2I, V3I, V4I)
        val booleans = arrayOf(V1B, V2B, V3B, V4B)
    }
}
