package me.anno.gpu.shader.builder

import me.anno.gpu.shader.GLSLType

// layout(rgba8, binding = 1) restrict uniform image2D dst;
class ExtendedVariable(type: GLSLType, name: String): Variable(type, name) {
    // todo make this class be used by Compute shaders for more complex things
}