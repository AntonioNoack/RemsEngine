package me.anno.graph.render.effects

import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.render.Texture
import me.anno.graph.types.flow.FlowGraphNodeUtils.getFloatInput
import me.anno.graph.types.flow.actions.ActionNode
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max

class OutlineNode : ActionNode(
    "Outline",
    listOf(
        "Float", "Strength",
        "Float", "Offset",
        "Vector4f", "Color",
        "Vector3f", "Weights",
        "Texture", "Diffuse",
        "Texture", "Normal",
        "Texture", "Depth",
        "Texture", "Illuminated",
    ), listOf("Texture", "Illuminated")
) {

    init {
        setInput(1, 1.0f)
        setInput(2, 0.5f)
        setInput(3, Vector4f(0f, 0f, 0f, 1f))
        setInput(4, Vector3f(1f))
    }

    private val framebuffer = Framebuffer(name, 1, 1, arrayOf(TargetType.Float16x3))

    override fun onDestroy() {
        super.onDestroy()
        framebuffer.destroy()
    }

    override fun executeAction() {

        val strength = getFloatInput(1)
        val offset = getFloatInput(2)
        val outlineColor = getInput(3) as Vector4f
        val weights = getInput(4) as Vector3f
        val color = ((getInput(5) as? Texture)?.tex as? Texture2D) ?: blackTexture
        val normalT = getInput(6) as? Texture
        val normalZW = normalT?.mapping == "zw"
        val normal = (normalT?.tex as? Texture2D) ?: blackTexture
        val depth = ((getInput(7) as? Texture)?.tex as? Texture2D) ?: whiteTexture
        val illT = getInput(8) as? Texture
        val illuminated = ((illT)?.tex as? Texture2D) ?: whiteTexture

        if (strength <= 0f) { // disabled
            setOutput(1, illT)
            return
        }

        val w = max(max(illuminated.width, color.width), max(normal.width, depth.width))
        val h = max(max(illuminated.height, color.height), max(normal.height, depth.height))
        useFrame(w, h, true, framebuffer, copyRenderer) {
            val shader = shader
            shader.use()
            color.bindTrulyNearest(shader, "colorTex")
            normal.bindTrulyNearest(shader, "normalTex")
            depth.bindTrulyNearest(shader, "depthTex")
            illuminated.bindTrulyNearest(shader, "illuminatedTex")
            shader.v2f("strength", strength, offset)
            shader.v2f("duv", 1f / w, 1f / h)
            shader.v1b("normalZW", normalZW)
            shader.v4f("outlineColor", outlineColor)
            val w0 = 1f / 16f // 4x weight by sobel filter, x² because of dot/x*x
            shader.v3f(
                "weights",
                if (color == whiteTexture || color == blackTexture) 0f else weights.x * w0,
                if (normal == whiteTexture || normal == blackTexture) 0f else weights.y * w0,
                if (depth == whiteTexture || depth == blackTexture) 0f else weights.z * w0
            )
            SimpleBuffer.flat01.draw(shader)
        }
        setOutput(1, Texture(framebuffer.getTexture0()))
    }

    companion object {
        val shader = Shader(
            "outline", ShaderLib.coordsList, ShaderLib.coordsUVVertexShader, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V2F, "duv"),
                Variable(GLSLType.V2F, "strength"),
                Variable(GLSLType.V1B, "normalZW"),
                Variable(GLSLType.V3F, "weights"),
                Variable(GLSLType.V4F, "outlineColor"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "normalTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.S2D, "illuminatedTex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    octNormalPacking +
                    "void main(){\n" +
                    // sobel filter
                    "#define SX S(uv0)+S(uv1)*2.0+S(uv2)-(S(uv5)+S(uv6)*2.0+S(uv7))\n" +
                    "#define SY S(uv0)+S(uv3)*2.0+S(uv5)-(S(uv2)+S(uv4)*2.0+S(uv7))\n" +
                    // define all uvs
                    "   vec2 uv0 = uv - duv, uv1 = uv - vec2(0.0,duv.y), uv2 = uv + vec2(duv.x,-duv.y),\n" +
                    "        uv3 = uv - vec2(duv.x,0.0), uv4 = uv + vec2(duv.x,0.0),\n" +
                    "        uv5 = uv - vec2(duv.x,-duv.y), uv6 = uv + vec2(0.0,duv.y), uv7 = uv + duv;\n" +
                    "   float outline = 0.0;\n" +
                    "   if(weights.z != 0.0){\n" +
                    "       #define S(u) log2(max(texture(depthTex,u).x,1e-38))\n" +
                    "       float dx = SX, dy = SY;\n" +
                    "       #undef S\n" +
                    "       outline += weights.z * (dx*dx+dy*dy);\n" +
                    "   }\n" +
                    "   if(weights.y != 0.0){\n" +
                    "       #define S(u) UnpackNormal(normalZW ? texture(normalTex,u).zw : texture(normalTex,u).xy)\n" +
                    "       vec3 nx = SX, ny = SY;\n" +
                    "       #undef S\n" +
                    "       outline += weights.y * (dot(nx,nx)+dot(ny,ny));\n" +
                    "   }\n" +
                    "   if(weights.x != 0.0){\n" +
                    "       #define S(u) texture(colorTex,u)\n" +
                    "       vec4 cx = SX, cy = SY;\n" +
                    "       #undef S\n" +
                    "       outline += weights.x * (dot(cx,cx)+dot(cy,cy));\n" +
                    "   }\n" +
                    "   outline = strength.x * sqrt(clamp(outline-strength.y,0.0,1.0));\n" +
                    "   result = mix(texture(illuminatedTex,uv), outlineColor, outline);\n" +
                    "}\n"
        )
    }
}