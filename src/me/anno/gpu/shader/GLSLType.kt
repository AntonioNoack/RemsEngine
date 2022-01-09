package me.anno.gpu.shader

enum class GLSLType(val glslName: String, val components: Int) {
    // the names could be improved
    BOOL("bool", 1),
    V1I("int", 1),
    V2I("ivec2", 2),
    V3I("ivec3", 3),
    V4I("ivec4", 4),
    V1F("float", 1),
    V2F("vec2", 2),
    V3F("vec3", 3),
    V4F("vec4", 4),
    M3x3("mat3", 9),
    M4x3("mat4x3", 12),
    M4x4("mat4", 16),
    S2D("sampler2D", 1000),
    SCube("samplerCube", 1000);

    override fun toString(): String = glslName
}
