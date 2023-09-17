package me.anno.gpu.shader.builder

enum class VariableMode(val glslName: String) {
    /** shader stage inout, typically uniforms or the stage before it */
    IN("in"),
    /** shader input, by vertex buffers like coords, normals, ... */
    ATTR("attr"),
    /** shader stage output */
    OUT("out"),
    /** shader stage processing: input and output */
    INOUT("inout"),
    /** shader stage processing, but the result will not be considered to be written if this is the last stage */
    INMOD("inmod"),
}