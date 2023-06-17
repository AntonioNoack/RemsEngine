package me.anno.tests.shader

import me.anno.Engine
import me.anno.gpu.GFX.flat01
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing

// get rain effect working like snow in https://www.glslsandbox.com/e#36547.0
fun main() {
    val shader = Shader(
        "rain", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.V1F, "time"),
            Variable(GLSLType.V2F, "uvScale"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                "float rain(vec2 uv, float scale) {\n" +
                "   float w=smoothstep(1.,0.,-uv.y*(scale/10.));if(w<.1)return 0.;\n" +
                "   uv+=time/scale;uv.y+=time*8./scale;uv.x+=sin(uv.y+time*.05)/scale;\n" +
                "   uv*=scale;vec2 s=floor(uv),f=fract(uv),p;float k=3.,d;\n" +
                "   p=.5+.35*sin(11.*fract(sin((s+p+scale)*mat2(7,3,6,5))*5.))-f;d=length(p);k=min(d,k);\n" +
                "   k=smoothstep(0.,k,sin(f.x+f.y)*0.01);\n" +
                "   return k*w;\n" +
                "}\n" +
                "void main(){\n" +
                "   vec2 uv2 = vec2(3.0,2.0) * uv * uvScale;\n" +
                "   float c=0.0;\n" +
                "   c+=rain(uv2,30.)*.3;\n" +
                "   c+=rain(uv2,20.)*.5;\n" +
                "   c+=rain(uv2,15.)*.8;\n" +
                "   c+=rain(uv2,10.);\n" +
                "   c+=rain(uv2,8.);\n" +
                "   c+=rain(uv2,6.);\n" +
                "   c+=rain(uv2,5.);\n" +
                "   result = vec4(c,c,c,1);\n" +
                "}"
    )
    testDrawing {
        shader.use()
        shader.v1f("time", Engine.gameTimeF)
        shader.v2f("uvScale", it.w.toFloat() / it.h, 1f)
        flat01.draw(shader)
    }
}