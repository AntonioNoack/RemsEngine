package me.anno.tests.gfx

import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.ShaderPrinting
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import org.apache.logging.log4j.LogManager

// todo somewhere is undefined behavior :(, it only sometimes prints things
fun main() {
    LogManager.disableInfoLogs("LWJGLDebugCallback")
    val shader = Shader(
        "printTest", emptyList(), coordsUVVertexShader, uvList,
        listOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                ShaderPrinting.PRINTING_LIB +
                ShaderPrinting.definePrintCall(listOf(GLSLType.V2F, GLSLType.V1F)) +
                "void main() {\n" +
                "   float len = fract(1.0 / length(uv));\n" +
                "   if(len < 1e-5) println(\"Found value: (%f,%f) -> %f\", uv, len);\n" +
                "   result = vec4(vec3(len), 1.0);\n" +
                "}\n"
    )
    testDrawing("Shader Printing Test") {
        shader.use()
        flat01.draw(shader)
    }
}