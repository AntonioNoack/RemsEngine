package me.anno.games.carchase

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.disableSubpixelRendering
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.drawCircle
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer.Companion.colorRenderer
import me.anno.gpu.texture.ITexture2D
import me.anno.maths.Maths.PIf
import me.anno.ui.Panel
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MinimapPanel : Panel(style) {

    override fun calculateSize(w: Int, h: Int) {
        minW = 200
        minH = 200
    }

    private val fb = Framebuffer(
        "Minimap", 1, 1, 1,
        TargetType.UInt8x4, DepthBufferType.INTERNAL
    )

    private val pos = Vector3d(0.0, 10.0, 0.0)
    private val rot = Quaternionf()
    private val cam = Matrix4f()
    private val near = 0.1f
    private val far = 1e3f
    private val fovRadians = 1.4f
    private val aspect = 1f

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

        val rv = RenderView.currentInstance ?: return
        val pipeline = rv.pipeline

        val size = min(width, height)

        pos.set(rv.cameraPosition)
        pos.y += 100.0

        val angle = rv.cameraRotation.getEulerAngleYXZvY()
        rot.rotationY(angle)
            .rotateX(-PIf * 0.4f)

        pipeline.frustum.definePerspective(
            near, far, fovRadians, 10,
            aspect, pos, rot
        )
        pipeline.fill(rv.getWorld() ?: return)

        Perspective.setPerspective(
            cam, fovRadians,
            aspect, near, far, 0f, 0f
        )
        cam.rotateInv(rot)

        RenderState.cameraMatrix.set(cam)
        RenderState.cameraPosition.set(pos)
        RenderState.cameraRotation.set(rot)
        RenderState.calculateDirections(isPerspective = true, needsInversion = true)

        // render scene from top onto FB
        rv.drawScene(
            size, size, colorRenderer, fb,
            changeSize = true, hdr = false, sky = true
        )

        val r = size * 0.5f
        drawTextureCircle(x, y, width, height, fb.getTexture0())
        drawCircle(
            x + r, y + r, r, r,
            0.9f, 0f, 360f, white
        )

        val dirs = "NESW"
        val dsr = disableSubpixelRendering
        disableSubpixelRendering = true
        for (i in dirs.indices) {
            val f = r * 0.93f
            val angleI = angle + PIf * i * 0.5f
            DrawTexts.drawSimpleTextCharByChar(
                (x + r - f * sin(angleI)).toInt(), (y + r + f * cos(angleI)).toInt(),
                1, dirs[i].toString(), black, white.withAlpha(0),
                AxisAlignment.CENTER, AxisAlignment.CENTER,
            )
        }
        disableSubpixelRendering = dsr
    }

    companion object {

        private fun drawTextureCircle(x: Int, y: Int, w: Int, h: Int, texture: ITexture2D) {
            GFX.check()
            val shader = circleTexShader
            shader.use()
            posSize(shader, x, y, w, h, true)
            GFXx2D.tiling(shader, null)
            texture.bind(0)
            flat01.draw(shader)
            GFX.check()
        }

        private val circleTexShader = Shader(
            "circleTexShader",
            ShaderLib.uiVertexShaderList,
            ShaderLib.uiVertexShader, uvList,
            listOf(
                Variable(GLSLType.S2D, "tex"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "vec3 get(vec2 uv, vec2 du, vec2 dv, ivec2 dir){\n" +
                    "   return texture(tex,uv+du*float(dir.x)+dv*float(dir.y)).rgb;\n" +
                    "}\n" +
                    "void main() {\n" +

                    "   if(length(uv-0.5) > 0.47) discard;\n" +
                    "   vec2 du = dFdx(uv), dv = dFdy(uv);\n" +

                    "   vec3 dx =\n" +
                    "       (get(uv,du,dv,ivec2(-1,-1)) - get(uv,du,dv,ivec2(1,-1))) +\n" +
                    "       (get(uv,du,dv,ivec2(-1, 0)) - get(uv,du,dv,ivec2(1, 0))) * 2.0 +\n" +
                    "       (get(uv,du,dv,ivec2(-1,+1)) - get(uv,du,dv,ivec2(1,+1)));\n" +

                    "   vec3 dy =\n" +
                    "       (get(uv,du,dv,ivec2(-1,-1)) - get(uv,du,dv,ivec2(-1,+1))) +\n" +
                    "       (get(uv,du,dv,ivec2( 0,-1)) - get(uv,du,dv,ivec2( 0,+1))) * 2.0 +\n" +
                    "       (get(uv,du,dv,ivec2(+1,-1)) - get(uv,du,dv,ivec2(+1,+1)));\n" +

                    "   vec3 color = texture(tex, uv).xyz;\n" +
                    "   float edge = length(dx) + length(dy);\n" +
                    "   edge = clamp(0.1 * edge, 0.0, 1.0);\n" +
                    "   color = color * 0.3 + 0.7 * mix(vec3(0.8), vec3(0.1), edge);\n" +
                    "   result = vec4(color,1.0);\n" +
                    "}"
        )
    }
}