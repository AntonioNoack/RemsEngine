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
import me.anno.gpu.shader.effects.FXAA
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask1Index
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.maths.Maths.max
import me.anno.maths.Maths.posMod
import me.anno.utils.structures.lists.LazyList
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import org.joml.Matrix4f
import org.joml.Vector2f

/**
 * temporal edge reconstruction: smooths harsh pixelated lines
 * todo small test scene with slow swinging
 * todo ensure ghosting is low, but it's still being properly anti-aliased
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
        val color = (getInput(1) as? Texture).texOrNull
        val motion = (getInput(2) as? Texture).texOrNull
        val maskTex0 = getInput(3) as? Texture
        val maskTex = maskTex0.texOrNull ?: whiteTexture
        if (color != null && motion != null) {
            timeRendering(name, timer) {
                val fi = RenderState.viewIndex * 2
                val fri = Time.frameIndex.hasFlag(1)
                val src = previous[fi + fri.toInt(0, 1)]
                val dst = previous[fi + fri.toInt(1, 0)]
                val maxTAA = getCameraSteadiness()
                useFrame(color.width, color.height, true, dst, copyRenderer) {
                    if (maxTAA > 0.01f) {
                        val shader = shader[maskTex0.mask1Index]
                        shader.use()
                        val srcT = if (src.isCreated()) src.getTexture0() else color
                        srcT.bindTrulyNearest(shader, "colorTex0")
                        color.bindTrulyNearest(shader, "colorTex1")
                        motion.bindTrulyNearest(shader, "motionTex")
                        maskTex.bindTrulyNearest(shader, "maskTex")
                        val jitter = getPattern(0) - getPattern(-1)
                        shader.v2f("jitter", jitter.mul(1f / src.width, 1f / src.height))
                        shader.v1f("maxTAA", maxTAA)
                        flat01.draw(shader)
                    } else {
                        FXAA.render(color)
                    }
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

        /**
         * returns 0.98 if steady, 0 if moving
         * */
        fun getCameraSteadiness(): Float {
            val distance = RenderState.cameraPosition.distance(RenderState.prevCameraPosition)
            return max(0.98f - 20f * distance.toFloat(), 0f)
        }

        val shader = LazyList(4) {
            val maskMask = "xyzw"[it]
            Shader(
                "TAA", emptyList(), coordsUVVertexShader, uvList, listOf(
                    Variable(GLSLType.V4F, "result", VariableMode.OUT),
                    Variable(GLSLType.S2D, "colorTex1"),
                    Variable(GLSLType.S2D, "colorTex0"),
                    Variable(GLSLType.S2D, "motionTex"),
                    Variable(GLSLType.S2D, "maskTex"),
                    Variable(GLSLType.V1B, "showEdges"),
                    Variable(GLSLType.V1F, "threshold"),
                    Variable(GLSLType.V2F, "jitter"),
                    Variable(GLSLType.V1F, "maxTAA")
                ), "void main(){" +
                        "   vec3 base = texture(colorTex1,uv).rgb;\n" +
                        "   vec2 motion0 = texture(motionTex,uv).xy;\n" +
                        "   vec2 motion = motion0 * 0.5 + jitter;\n" +
                        "   vec2 uv2 = uv-motion.xy;\n" +
                        "   vec2 cost = 50.0 * (\n" + // try to find neighboring motion
                        "        abs(textureOffset(motionTex,uv,ivec2(+1,0)).xy-motion0) +\n" +
                        "        abs(textureOffset(motionTex,uv,ivec2(-1,0)).xy-motion0) +\n" +
                        "        abs(textureOffset(motionTex,uv,ivec2(0,+1)).xy-motion0) +\n" +
                        "        abs(textureOffset(motionTex,uv,ivec2(0,-1)).xy-motion0) +\n" +
                        "        abs(textureOffset(motionTex,uv,ivec2(+3,0)).xy-motion0) +\n" +
                        "        abs(textureOffset(motionTex,uv,ivec2(-3,0)).xy-motion0) +\n" +
                        "        abs(textureOffset(motionTex,uv,ivec2(0,+3)).xy-motion0) +\n" +
                        "        abs(textureOffset(motionTex,uv,ivec2(0,-3)).xy-motion0)" +
                        "   ) + abs(motion.xy);\n" +
                        "   float f = maxTAA * texture(maskTex,uv).$maskMask - 250.0 * length(cost);\n" +
                        "   if(f > 0.0 && uv2.x >= 0.0 && uv2.y >= 0.0 && uv2.x <= 1.0 && uv2.y <= 1.0){\n" +
                        "       base = mix(base,texture(colorTex0,uv2).rgb,vec3(f));\n" +
                        "   }\n" +
                        "   result = vec4(base,1.0);\n" +
                        //"   result = vec4(f,f,f,1.0);\n" +
                        "}\n"
            )
        }

        val pattern = listOf(
            Vector2f(-0.37f, -0.37f),
            Vector2f(+0.12f, +0.12f),
            Vector2f(+0.12f, -0.37f),
            Vector2f(-0.37f, +0.12f),
            Vector2f(-0.12f, -0.12f),
            Vector2f(+0.37f, +0.37f),
            Vector2f(+0.37f, -0.12f),
            Vector2f(-0.12f, +0.37f),
        )

        val views = LazyMap { _: Int -> Matrix4f() }

        fun getPattern(dt: Int): Vector2f {
            return pattern[posMod(Time.frameIndex + dt, pattern.size)]
        }

        fun jitterAndStore(m: Matrix4f, pw: Int, ph: Int) {
            val pattern = getPattern(0)
            val jx = pattern.x
            val jy = pattern.y
            // if the camera is moving, we don't need jitter
            val amplitude = 2f * getCameraSteadiness()
            views[RenderState.viewIndex].set(m)
            jitter(m, jx, jy, amplitude, pw, ph)
        }

        fun jitter(m: Matrix4f, jx: Float, jy: Float, amplitude: Float, pw: Int, ph: Int) {
            val dx = jx * amplitude / pw
            val dy = jy * amplitude / ph
            jitter(m, dx, dy)
        }

        fun jitter(m: Matrix4f, dx: Float, dy: Float) {
            // why are the signs changing?
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