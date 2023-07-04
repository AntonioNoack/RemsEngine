package me.anno.gpu.shader

import me.anno.ecs.components.camera.effects.ColorMapEffect
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D

object RandomEffect : ColorMapEffect() {

    const val randomFunc = "#define GET_RANDOM(co) fract(sin(dot((co).xy, vec2(12.9898,78.233))) * 43758.547)\n"

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

    override fun render(color: ITexture2D): IFramebuffer {
        val target = FBStack["random", color.width, color.height, 4, false, 1, false]
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