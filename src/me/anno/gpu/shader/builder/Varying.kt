package me.anno.gpu.shader.builder

class Varying(val modifiers: String, val type: String, val name: String) {
    var vShaderName = name
    var fShaderName = name
    fun makeDifferent() {
        vShaderName = "v_$name"
        fShaderName = "f_$name"
    }
}