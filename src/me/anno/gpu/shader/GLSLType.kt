package me.anno.gpu.shader

enum class GLSLType(val glslName: String, val components: Int, val isFlat: Boolean) {
    // the names could be improved
    BOOL("bool", 1, true),
    V1I("int", 1, true),
    V2I("ivec2", 2, true),
    V3I("ivec3", 3, true),
    V4I("ivec4", 4, true),
    V1F("float", 1, false),
    V2F("vec2", 2, false),
    V3F("vec3", 3, false),
    V4F("vec4", 4, false),
    M3x3("mat3", 9, false),
    M4x3("mat4x3", 12, false),
    M4x4("mat4", 16, false),
    S2D("sampler2D", 1000, false),
    S3D("sampler3D", 1000, false),
    SCube("samplerCube", 1000, false);

    override fun toString(): String = glslName
}
