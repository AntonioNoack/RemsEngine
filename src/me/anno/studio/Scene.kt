package me.anno.studio

import de.javagl.jgltf.impl.v1.Camera
import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle
import me.anno.gpu.GFX
import me.anno.gpu.GFX.createCustomShader
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.Shader
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.input.Input.keysDown
import me.anno.objects.blending.BlendMode
import me.anno.objects.cache.Cache
import me.anno.objects.effects.BokehBlur
import me.anno.ui.editor.sceneView.Grid
import me.anno.video.MissingFrameException
import org.joml.Matrix4fStack
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30
import java.io.File

object Scene {

    lateinit var sqrtToneMappingShader: Shader
    lateinit var lutShader: Shader

    var isInited = false
    fun init(){

        // add randomness against banding
        val noiseFunc = "" +
                "float random(vec2 co){\n" +
                "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n" +
                "}\n"

        sqrtToneMappingShader = Shader("" +
                "in vec2 attr0;\n" +
                "uniform float ySign;\n" +
                "void main(){" +
                "   vec2 coords = attr0*2.0-1.0;\n" +
                "   gl_Position = vec4(coords.x, coords.y * ySign, 0.0, 1.0);\n" +
                "   uv = attr0;\n" +
                "}", "" +
                "varying vec2 uv;\n", "" +
                "uniform sampler2D tex;\n" +
                "uniform float minValue;\n" +
                noiseFunc +
                "void main(){" +
                "   gl_FragColor = sqrt(texture(tex, uv)) + random(uv) * minValue;\n" +
                "}")

        lutShader = createCustomShader("" +
                "in vec2 attr0;\n" +
                "uniform float ySign;\n" +
                "void main(){" +
                "   gl_Position = vec4(attr0*2.0-1.0, 0.0, 1.0);\n" +
                "   uv = attr0;\n" +
                "}", "" +
                "varying vec2 uv;\n", "" +
                "uniform sampler2D tex;\n" +
                "uniform sampler3D lut;\n" +
                // "uniform float time;\n" +
                noiseFunc +
                "void main(){" +
                "   vec4 c0 = texture(tex, uv);\n" +//vec4(uv, time, 1.0);//
                "   vec3 color = clamp(c0.rgb, 0.0, 1.0);\n" +
                "   gl_FragColor = vec4(texture(lut, color.rbg).rgb, c0.a);\n" +
                "}", listOf("tex", "lut"))

        isInited = true
    }

    fun switch(buffer: Framebuffer, offset: Int, nearest: Boolean): Framebuffer {
        val next = FBStack[buffer.w, buffer.h]
        next.bind()
        buffer.bindTextures(offset, nearest)
        return next
    }

    fun draw(target: Framebuffer?, x0: Int, y0: Int, w: Int, h: Int, time: Float, flipY: Boolean){

        GFX.check()

        if(!isInited) init()

        val camera = GFX.selectedCamera
        val (cameraTransform, cameraTime) = camera.getGlobalTransform(time)

        GFX.clip(x0, y0, w, h)

        var buffer = FBStack[w, h]
        buffer.bind()
        //framebuffer.bind(w, h)

        GFX.check()
        GFX.drawRect(x0, y0, w, h, DefaultStyle.black)

        if(camera.useDepth){
            GL30.glEnable(GL30.GL_DEPTH_TEST)
            GL30.glClearDepth(1.0)
            GL30.glDepthRange(-1.0, 1.0)
            GL30.glDepthFunc(GL30.GL_LEQUAL)
            GL30.glClear(GL30.GL_DEPTH_BUFFER_BIT)
        } else {
            GL30.glDisable(GL30.GL_DEPTH_TEST)
        }

        // draw the 3D stuff

        val stack = Matrix4fStack(256)

        GFX.applyCameraTransform(camera, cameraTime, cameraTransform, stack)

        val white = camera.color[cameraTime]

        stack.pushMatrix()
        Grid.draw(stack, cameraTransform)
        stack.popMatrix()

        if(camera.useDepth){
            GL30.glEnable(GL30.GL_DEPTH_TEST)
        } else {
            GL30.glDisable(GL30.GL_DEPTH_TEST)
        }

        BlendMode.DEFAULT.apply()
        GL30.glDepthMask(true)

        stack.pushMatrix()
        // root.draw(stack, editorHoverTime, Vector4f(1f,1f,1f,1f))
        GFX.nullCamera.draw(stack, time, white)
        stack.popMatrix()
        stack.pushMatrix()
        GFX.root.draw(stack, time, white)
        stack.popMatrix()

        // todo gizmos for orientation

        // this is manipulation the grid transform somehow -.-
        /*stack.pushMatrix()
        val x2 = stack.transformDirection(xAxis)
        val y2 = stack.transformDirection(yAxis)
        val z2 = stack.transformDirection(zAxis)
        val gizmoSize = 50f
        val gizmoPadding = 10f
        val gx = x0 + w - gizmoSize - gizmoPadding
        val gy = y0 + gizmoSize + gizmoPadding
        fun drawCircle(x: Float, y: Float){
            GFX.drawRect(
                (gx + gizmoSize * x - 5).toInt(),
                (gy + gizmoSize * y - 5).toInt(), 10, 10, -1)
        }
        drawCircle(x2.x, x2.y)
        drawCircle(y2.x, y2.y)
        drawCircle(z2.x, z2.y)
        stack.popMatrix()*/
        // todo display a 3D gizmo?

        BlendMode.DEFAULT.apply()

        glDisable(GL_DEPTH_TEST)



        val enableCircularDOF = 'K'.toInt() in keysDown
        if(enableCircularDOF){
            buffer = switch(buffer, 0, true)
            BokehBlur.draw(buffer, w, h, 0.02f)
        }

        fun bindTarget(){
            if(target == null){
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
                GFX.clip(x0, y0, w, h)
            } else {
                target.bind()
            }
        }

        val lutFile = camera.lut
        val needsLUT = lutFile.exists() && !lutFile.isDirectory
        val lut = if(needsLUT) Cache.getLUT(lutFile, 100_000) else null
        if(lut == null && needsLUT && isFinalRendering) throw MissingFrameException(File(""))

        val useLUT = lut != null
        if(useLUT){
            buffer = switch(buffer, 0, true)
        } else {
            bindTarget()
            buffer.bindTextures(0, true)
        }

        // outputTexture.bind(0, true)

        // todo apply tonemapping
        // todo add camera pseudo effects (red-blue-shift)
        sqrtToneMappingShader.use()
        sqrtToneMappingShader.v1("ySign", if(flipY) -1f else 1f)
        val colorDepth = DefaultConfig["display.colorDepth", 8]
        val minValue = 1f/(1 shl colorDepth)
        sqrtToneMappingShader.v1("minValue", minValue)
        flat01.draw(sqrtToneMappingShader)
        GFX.check()

        // todo at the end apply the lut

        if(useLUT){
            bindTarget()
            buffer.bindTextures(0, true)
            lutShader.use()
            lut!!.bind(1, false)
            lut.clamping(false)
            flat01.draw(lutShader)
            GFX.check()
        }

        FBStack.clear(w, h)

        glEnable(GL_BLEND)

    }

}