package me.anno.graph.visual.render.effects

import me.anno.ecs.systems.GlobalSettings
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.TextureLib.blackTexture
import kotlin.math.PI

class PaniniProjectionNode : TimedRenderingNode(
    "Panini Projection",
    listOf("Texture", "Illuminated"),
    listOf("Texture", "Illuminated")
) {

    override fun executeAction() {
        val color = getTextureInput(1)
            ?: return finish(blackTexture)

        timeRendering(name, timer) {
            val dst = FBStack[name, color.width, color.height, TargetType.UInt8x4, 1, DepthBufferType.NONE]
            useFrame(dst) {
                val shader = paniniShader
                shader.use()
                val settings = GlobalSettings[PaniniProjectionSettings::class]
                val aspect = color.width.toFloat() / color.height.toFloat()
                shader.v2f("aspectRatio", aspect, 1f / aspect)
                shader.v1f("fovFactor", RenderState.fovXRadians)
                shader.v1f("paniniDistance", settings.distance) // 0 = perspective?
                color.bindTrulyLinear(shader, "colorTex")
                flat01.draw(shader)
            }
            finish(dst.getTexture0())
        }
    }

    companion object {
        private val paniniShader = Shader(
            "Panini", emptyList(), coordsUVVertexShader, uvList, listOf(
                Variable(GLSLType.V2F, "aspectRatio"),
                Variable(GLSLType.V1F, "fov"),
                Variable(GLSLType.V1F, "verticalCompression"),
                Variable(GLSLType.V1F, "paniniDistance"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
            ), "" +
                    // we have pretty much three formulas for panini, and all are different,
                    //  and none work as I'd expect them too...

                    // https://www.shadertoy.com/view/Wt3fzB
                    // tc ∈ [-1,1]² | fov ∈ [0, π) | d ∈ [0,1]\n" +
                    "float Pow2(float f) { return f*f; }\n" +
                    "vec3 paniniProjection(vec2 tc, float fov, float d) {\n" +
                    "   float d2 = d*d;\n" +
                    "   float fo = ${PI * 0.5} - fov * 0.5;\n" +
                    "   float f = cos(fo)/sin(fo) * 2.0;\n" +
                    "   float f2 = f*f;\n" +
                    "   float b = (sqrt(max(0.0, Pow2(d+d2)*(f2+f2*f2))) - (d*f+f)) / (d2+d2*f2-1.0);\n" +
                    "   b = 2.5;\n" + // todo b produces incorrect values :(, why???
                    "   tc *= b;\n" +

                    // http://tksharpless.net/vedutismo/Pannini/panini.pdf
                    "    float h = tc.x;\n" +
                    "    float v = tc.y;\n" +

                    "    float h2 = h*h;\n" +

                    "    float k = h2/Pow2(d+1.0);\n" +
                    "    float k2 = k*k;\n" +

                    "    float discr = max(0.0, k2*d2 - (k+1.0)*(k*d2-1.0));\n" +
                    "    float cosPhi = (-k*d+sqrt(discr))/(k+1.0);\n" +
                    "    float S = (d+1.0)/(d+cosPhi);\n" +
                    "    float tanTheta = v/S;\n" +

                    "    float sinPhi = sqrt(max(0.0, 1.0-Pow2(cosPhi)));\n" +
                    "    if(tc.x < 0.0) sinPhi *= -1.0;\n" +

                    "    float s = inversesqrt(1.0+Pow2(tanTheta));\n" +
                    "    return vec3(sinPhi, tanTheta, cosPhi) * s;\n" +
                    "}\n" +

                    "void main() {\n" +
                    "   vec2 uvi = uv * 2.0 - 1.0;\n" +
                    "   uvi.x *= aspectRatio.x;\n" +
                    "   uvi = paniniProjection(uvi, fov, paniniDistance).xy;\n" +
                    "   uvi.x *= aspectRatio.y;\n" +
                    "   uvi = uvi * 0.5 + 0.5;\n" +
                    "   result = max(abs(uvi.x-0.5),abs(uvi.y-0.5))<=0.5 ? texture(colorTex, uvi) : vec4(0.0);\n" +
                    "}\n"
        )
    }
}