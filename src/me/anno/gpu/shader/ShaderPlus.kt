package me.anno.gpu.shader

import me.anno.ecs.components.camera.effects.ColorMapEffect
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D

object ShaderPlus {

    const val randomFunc = "#define GET_RANDOM(co) fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453)\n"

    fun createShaderStage(hasTint: Boolean = true): ShaderStage {
        val callName = "applyShaderPlus"
        val variables = listOf(
            Variable(GLSLType.V1F, "zDistance"),
            Variable(GLSLType.V4F, "tint"),
            Variable(GLSLType.V3F, "finalColor"),
            Variable(GLSLType.V1F, "finalAlpha"),
            Variable(GLSLType.V1I, "drawMode"),
            Variable(GLSLType.V1I, "randomId"),
            Variable(GLSLType.V2F, "finalMotion"),
            Variable(GLSLType.V4F, "SPResult", VariableMode.OUT),
        )
        val code = "" +
                randomFunc +
                "switch(drawMode){\n" +
                "   case ${DrawMode.COLOR_SQUARED.id}:\n" +
                "       vec3 tmpCol = ${if (hasTint) "finalColor" else "finalColor\n" +
                        "#ifndef IS_TINTED\n" +
                        " * tint.rgb\n" +
                        "#endif\n"};\n" +
                "       SPResult = vec4(tmpCol * tmpCol, clamp(finalAlpha, 0.0, 1.0) * tint.a);\n" +
                "       break;\n" +
                "   case ${DrawMode.COLOR.id}:\n" +
                "       SPResult = vec4(${
                    if (hasTint) "finalColor" else "finalColor\n" +
                            "#ifndef IS_TINTED\n" +
                            " * tint.rgb\n" +
                            "#endif\n"
                }, clamp(finalAlpha\n" +
                "#ifndef IS_TINTED\n" +
                " * tint.a\n" +
                "#endif\n, 0.0, 1.0));\n" +
                "       break;\n" +
                "   case ${DrawMode.COPY.id}:\n" +
                "      SPResult = vec4(finalColor, finalAlpha);\n" +
                "      break;\n" +
                "   default:\n" +
                "       SPResult = vec4(1.0,0.0,0.5,1.0);\n" +
                "       break;\n" +
                "}\n"
        return ShaderStage(callName, variables, code)
    }

    val randomShader = Shader(
        "random", coordsList, coordsVShader, uvList,
        listOf(Variable(GLSLType.S2D, "source")), "" +
                randomFunc +
                "void main(){\n" +
                "   vec4 tint = texture(source, uv);\n" +
                "   float flRandomId2 = dot(vec4(256.0*65536.0, 65536.0, 256.0, 1.0), tint);\n" +
                "   vec2 seed2 = vec2(sin(flRandomId2), cos(flRandomId2));\n" +
                "   gl_FragColor = vec4(GET_RANDOM(seed2.xy), GET_RANDOM(seed2.yx), GET_RANDOM(100.0 - seed2.yx), 1.0);\n" +
                "\n}"
    )

    object RandomEffect : ColorMapEffect() {
        override fun render(color: ITexture2D): IFramebuffer {
            val target = FBStack["random", color.w, color.h, 4, false, 1, false]
            useFrame(target) {
                val shader = randomShader
                shader.use()
                color.bind(0, GPUFiltering.NEAREST, Clamping.CLAMP)
                flat01.draw(shader)
            }
            return target
        }

        override fun clone() = this // illegal
    }

    // todo change everything from gl_FragColor or fractColor to finalColor, finalAlpha,
    //  and then use renderers, and delete this DrawMode-legacy-stuff
    @Deprecated("This will soon be removed; use Renderers instead")
    enum class DrawMode(val id: Int) {
        COLOR_SQUARED(0),
        COLOR(1),
        COPY(6),
    }

}