package me.anno.gpu.shader

import me.anno.gpu.GFX
import java.lang.RuntimeException

/**
 * a pair of a shader that is rendered normally,
 * and a shader, which renders a pure color for object click detection
 * */
class ShaderPair(vertex: String, varying: String, fragment: String){

    val correctShader = Shader(vertex, varying, fragment)
    val monoShader = Shader(vertex, varying, makeMono(fragment))

    val shader get() = if(GFX.isFakeColorRendering) monoShader else correctShader

    fun makeMono(shader: String): String {
        val raw = shader.trim()
        if(!raw.endsWith("}")) throw RuntimeException()
        return raw.substring(0, raw.length-1) + "" +
                "   if(gl_FragColor.a < 0.01) discard;\n" +
                "   gl_FragColor.rgb = (tint*tint).rgb;\n" +
                "}"
    }

}