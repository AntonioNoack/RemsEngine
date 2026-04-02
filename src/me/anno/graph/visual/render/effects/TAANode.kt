package me.anno.graph.visual.render.effects

import me.anno.Time
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.render.Texture
import me.anno.maths.Maths.halton
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import org.joml.Matrix4f
import org.joml.Vector2f

/**
 * temporal edge reconstruction: smooths harsh pixelated lines
 * */
class TAANode : TimedRenderingNode(
    "TAA",
    listOf("Texture", "Illuminated", "Texture", "Motion", "Texture", "Mask"),
    listOf("Texture", "Illuminated")
) {

    private val previous = LazyMap { _: Int ->
        Framebuffer("taa", 1, 1, 1, TargetType.UInt8x3, DepthBufferType.NONE)
    }

    override fun executeAction() {
        val color = getTextureInput(1)
        val motion = getTextureInput(2)
        val maskTex = getTextureInput(3, whiteTexture)
        val maskMask = getTextureInputMask(3)
        if (color != null && motion != null) {
            timeRendering(name, timer) {
                val fi = RenderState.viewIndex * 2
                val fri = Time.frameIndex.hasFlag(1)
                val src = previous[fi + fri.toInt(0, 1)]
                val dst = previous[fi + fri.toInt(1, 0)]
                useFrame(color.width, color.height, true, dst, copyRenderer) {
                    val shader = shader
                    shader.use()
                    val srcT = if (src.isCreated()) src.getTexture0() else color
                    srcT.bindTrulyNearest(shader, "colorTex0")
                    color.bindTrulyNearest(shader, "colorTex1")
                    motion.bindTrulyNearest(shader, "motionTex")
                    maskTex.bindTrulyNearest(shader, "maskTex")
                    val deltaJitter = getPatternDiff(-1, 0, tmp0, tmp1)
                    shader.v2f("jitter", deltaJitter.mul(1f / src.width, 1f / src.height))
                    shader.v4f("maskMask", maskMask)
                    flat01.draw(shader)
                }
                unjitter(RenderState.cameraMatrix)
                setOutput(1, Texture.texture(dst, 0))
            }
        } else setOutput(1, getInput(1))
    }

    override fun destroy() {
        super.destroy()
        for (v in previous.values) {
            v.destroy()
        }
    }

    companion object {

        private val tmp0 = Vector2f()
        private val tmp1 = Vector2f()

        val shader = Shader(
            "TAA", emptyList(), coordsUVVertexShader, uvList, listOf(
                Variable(GLSLType.V4F, "result", VariableMode.OUT),
                Variable(GLSLType.S2D, "colorTex1"),
                Variable(GLSLType.S2D, "colorTex0"),
                Variable(GLSLType.S2D, "motionTex"),
                Variable(GLSLType.S2D, "maskTex"),
                Variable(GLSLType.V4F, "maskMask"),
                Variable(GLSLType.V1B, "showEdges"),
                Variable(GLSLType.V1F, "threshold"),
                Variable(GLSLType.V2F, "jitter")
            ), "void main(){" +
                    "   vec3 base = texture(colorTex1,uv).rgb;\n" +
                    "   vec2 motion0 = texture(motionTex,uv).xy;\n" +
                    "   vec2 motion = (motion0 + jitter) * 0.5;\n" +
                    "   vec2 uv2 = uv-motion.xy;\n" +
                    "   float velocity = length(motion0);\n" +
                    "   float rejection = smoothstep(0.0, 0.01, velocity);" +
                    "   float f = (1.0 - rejection) * dot(texture(maskTex,uv), maskMask);\n" +
                    "   if (f > 0.0 && uv2.x >= 0.0 && uv2.y >= 0.0 && uv2.x <= 1.0 && uv2.y <= 1.0) {\n" +
                    "       vec3 history = texture(colorTex0, uv2).rgb;\n" +
                    "       vec3 minC = base, maxC = base, c;\n" +

                    "       c = textureOffset(colorTex1, uv, ivec2(-1,-1)).rgb; minC = min(minC, c); maxC = max(maxC, c);\n" +
                    "       c = textureOffset(colorTex1, uv, ivec2(-1, 0)).rgb; minC = min(minC, c); maxC = max(maxC, c);\n" +
                    "       c = textureOffset(colorTex1, uv, ivec2(-1,+1)).rgb; minC = min(minC, c); maxC = max(maxC, c);\n" +
                    "       c = textureOffset(colorTex1, uv, ivec2( 0,-1)).rgb; minC = min(minC, c); maxC = max(maxC, c);\n" +
                    "       c = textureOffset(colorTex1, uv, ivec2( 0,+1)).rgb; minC = min(minC, c); maxC = max(maxC, c);\n" +
                    "       c = textureOffset(colorTex1, uv, ivec2(+1,-1)).rgb; minC = min(minC, c); maxC = max(maxC, c);\n" +
                    "       c = textureOffset(colorTex1, uv, ivec2(+1, 0)).rgb; minC = min(minC, c); maxC = max(maxC, c);\n" +
                    "       c = textureOffset(colorTex1, uv, ivec2(+1,+1)).rgb; minC = min(minC, c); maxC = max(maxC, c);\n" +

                    "       history = clamp(history, minC, maxC);\n" +
                    "       base = mix(base, history, f);\n" +
                    "   }\n" +
                    "   result = vec4(base,1.0);\n" +
                    //"   result = vec4(f,f,f,1.0);\n" +
                    "}\n"
        )

        val views = LazyMap { _: Int -> Matrix4f() }

        fun getPattern(dt: Int, dst: Vector2f): Vector2f {
            val t = Time.frameIndex + dt
            return dst.set(
                halton(t, 2) * 2f - 1f,
                halton(t, 3) * 2f - 1f
            )
        }

        fun getPatternDiff(dt0: Int, dt1: Int, tmp: Vector2f, dst: Vector2f): Vector2f {
            return getPattern(dt1, dst).sub(getPattern(dt0, tmp))
        }

        fun jitterAndStore(m: Matrix4f, pw: Int, ph: Int) {
            val pattern = getPattern(0, tmp0)
            val jx = pattern.x
            val jy = pattern.y
            views[RenderState.viewIndex].set(m)
            jitter(m, jx, jy, 1f, pw, ph)
        }

        fun jitter(m: Matrix4f, jx: Float, jy: Float, amplitude: Float, pw: Int, ph: Int) {
            val dx = jx * amplitude / pw
            val dy = jy * amplitude / ph
            jitter(m, dx, dy)
        }

        fun jitter(m: Matrix4f, dx: Float, dy: Float) {
            // why are the signs changing? because z is reversed?
            if (RenderState.isPerspective) {
                m._m20(m.m20 + dx)
                m._m21(m.m21 + dy)
            } else {
                m._m30(m.m30 - dx)
                m._m31(m.m31 - dy)
            }
            m.determineProperties()
        }

        fun unjitter(m: Matrix4f) {
            m.set(views[RenderState.viewIndex])
                .rotateInv(RenderState.cameraRotation)
        }
    }
}