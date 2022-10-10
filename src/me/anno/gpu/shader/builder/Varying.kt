package me.anno.gpu.shader.builder

import me.anno.gpu.shader.GLSLType

class Varying(val modifiers: String, val type: GLSLType, val name: String) {
    val vShaderName = name
    val fShaderName = name
}