package me.anno.gpu.shader.builder

import me.anno.gpu.shader.GLSLType

class Varying(val modifiers: String, val type: GLSLType, val name: String) {
    var vShaderName = name
    var fShaderName = name
    fun makeDifferent() {
        vShaderName = "v_$name"
        fShaderName = "f_$name"
    }
}