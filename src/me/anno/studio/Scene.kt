package me.anno.studio

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
import me.anno.io.json.testModelRendering
import me.anno.objects.Camera
import me.anno.objects.blending.BlendMode
import me.anno.objects.cache.Cache
import me.anno.objects.effects.BokehBlur
import me.anno.objects.effects.ToneMappers
import me.anno.ui.editor.sceneView.Grid
import me.anno.utils.times
import me.anno.video.MissingFrameException
import org.joml.Matrix4fStack
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30
import java.io.File
import kotlin.math.sqrt

object Scene {

    lateinit var sqrtToneMappingShader: Shader
    lateinit var lutShader: Shader

    var isInited = false
    fun init(){

        // add randomness against banding
        val noiseFunc = "" +
                "float random(vec2 co){\n" +
                "    return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453);\n" +
                "}\n"

        val reinhardToneMapping = "" +
                "vec3 reinhard(vec3 color){\n" +
                "   return color / (color + 1.0);\n" +
                "}\n"

        val reinhard2ToneMapping = "" +
                "vec3 reinhard(vec3 color){\n" +
                "   const float invWhiteSq = ${1.0/16.0};\n" +
                "   return (color * (1.0 + color * invWhiteSq)) / (color + 1.0);\n" +
                "}\n"

        val uchimuraToneMapping = "" +
                // Uchimura 2017, "HDR theory and practice"
                // Math: https://www.desmos.com/calculator/gslcdxvipg
                // Source: https://www.slideshare.net/nikuque/hdr-theory-and-practicce-jp
                // source^2: https://github.com/dmnsgn/glsl-tone-map/blob/master/uchimura.glsl
                "vec3 uchimura(vec3 x, float P, float a, float m, float l, float c, float b) {\n" +
                "  float l0 = ((P - m) * l) / a;\n" +
                "  float L0 = m - m / a;\n" +
                "  float L1 = m + (1.0 - m) / a;\n" +
                "  float S0 = m + l0;\n" +
                "  float S1 = m + a * l0;\n" +
                "  float C2 = (a * P) / (P - S1);\n" +
                "  float CP = -C2 / P;\n" +
                "\n" +
                "  vec3 w0 = vec3(1.0 - smoothstep(0.0, m, x));\n" +
                "  vec3 w2 = vec3(step(m + l0, x));\n" +
                "  vec3 w1 = vec3(1.0 - w0 - w2);\n" +
                "\n" +
                "  vec3 T = vec3(m * pow(x / m, vec3(c)) + b);\n" +
                "  vec3 S = vec3(P - (P - S1) * exp(CP * (x - S0)));\n" +
                "  vec3 L = vec3(m + a * (x - m));\n" +
                "\n" +
                "  return T * w0 + L * w1 + S * w2;\n" +
                "}\n" +
                "\n" +
                "vec3 uchimura(vec3 x) {\n" +
                "  const float P = 1.0;  // max display brightness\n" +
                "  const float a = 1.0;  // contrast\n" +
                "  const float m = 0.22; // linear section start\n" +
                "  const float l = 0.4;  // linear section length\n" +
                "  const float c = 1.33; // black\n" +
                "  const float b = 0.0;  // pedestal\n" +
                "\n" +
                "  return uchimura(x, P, a, m, l, c, b);\n" +
                "}"

        // academy color encoding system; e.g. used by UE4
        // says it shall be standard for the film industry
        val acesToneMapping = "" +
                // Narkowicz 2015, "ACES Filmic Tone Mapping Curve"
                // source^2: https://github.com/dmnsgn/glsl-tone-map/blob/master/aces.glsl
                "vec3 aces(vec3 x) {\n" +
                "   const float a = 2.51;\n" +
                "   const float b = 0.03;\n" +
                "   const float c = 2.43;\n" +
                "   const float d = 0.59;\n" +
                "   const float e = 0.14;\n" +
                "   return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);\n" +
                "}"

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
                "uniform vec3 fxScale;\n" +
                "uniform float chromaticAberration;" +
                "uniform vec2 chromaticOffset;\n" +
                "uniform vec2 distortion, distortionOffset;\n" +
                "uniform float minValue;\n" +
                "uniform float toneMapper;\n" +
                noiseFunc +
                reinhardToneMapping +
                acesToneMapping +
                uchimuraToneMapping +
                "vec2 distort(vec2 uv, vec2 nuv, vec2 duv){" +
                "   vec2 nuv2 = nuv + duv;\n" +
                "   float r2 = dot(nuv2,nuv2), r4 = r2*r2;\n" +
                "   vec2 uv2 = uv + duv + ((nuv2 - distortionOffset) * dot(distortion, vec2(r2, r4)))/fxScale.xy;\n" +
                "   return uv2;\n" +
                "}" +
                "vec4 getColor(vec2 uv){" +
                "   float zero = min(min(uv.x, 1.0-uv.x), min(uv.y, 1.0-uv.y))*1000.0;\n" +
                "   vec4 color = texture(tex, uv);\n" +
                "   return vec4(color.rgb, color.a * clamp(zero, 0.0, 1.0));\n" +
                "}" +
                "void main(){" +
                "   vec2 uv2 = (uv-0.5)*fxScale.z+0.5;\n" +
                "   vec2 nuv = (uv2-0.5)*fxScale.xy;\n" +
                "   vec2 duv = (chromaticAberration * nuv + chromaticOffset)/fxScale.xy;\n" +
                "   vec2 uvR = distort(uv2, nuv, duv), uvG = distort(uv2, nuv, vec2(0.0)), uvB = distort(uv2, nuv, -duv);\n" +
                "   vec2 ra = getColor(uvR).ra;\n" +
                "   vec2 ga = getColor(uvG).ga;\n" +
                "   vec2 ba = getColor(uvB).ba;\n" +
                "   vec4 raw = vec4(ra.x, ga.x, ba.x, ga.y);\n" +
                // "   float tm5 = 1.0 - dot(toneMappers, vec4(1.0));\n" +
                "   vec3 toneMapped = " +
                "       toneMapper < 0.5 ?" +
                "           raw.rgb : " +
                "       toneMapper < 1.5 ?" +
                "           reinhard(raw.rgb) :" +
                "       toneMapper < 2.5 ?" +
                "           aces(raw.rgb) :" +
                "           uchimura(raw.rgb);\n" +
                "   vec4 color = vec4(sqrt(toneMapped), raw.a);\n" +
                "   gl_FragColor = color + random(uv) * minValue;\n" +
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

    fun switch(buffer: Framebuffer, offset: Int, nearest: Boolean, withMultisampling: Boolean?): Framebuffer {
        val next = FBStack[buffer.w, buffer.h, withMultisampling ?: buffer.withMultisampling]
        next.bind()
        buffer.bindTextures(offset, nearest)
        return next
    }

    fun draw(target: Framebuffer?, camera: Camera, x0: Int, y0: Int, w: Int, h: Int, time: Float, flipY: Boolean){

        val enableMultisampling = false

        GFX.check()

        if(!isInited) init()

        GFX.usedCamera = camera

        val (cameraTransform, cameraTime) = camera.getGlobalTransform(time)

        GFX.clip(x0, y0, w, h)

        var buffer = FBStack[w, h, enableMultisampling]
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

        val white = Vector4f(1f, 1f, 1f, 1f)
        // camera.color[cameraTime]
        // use different settings for white balance etc...

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

        testModelRendering()

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
            buffer = switch(buffer, 0, true, withMultisampling = false)
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
            buffer = switch(buffer, 0, false, withMultisampling = false)
        } else {
            bindTarget()
            buffer.bindTextures(0, false)
        }

        /**
         * Tone Mapping, Distortion, and applying the sqrt operation (reverse pow(, 2.2))
         * */

        // outputTexture.bind(0, true)

        // todo render at higher resolution for distortion?
        // msaa should help, too
        // add camera pseudo effects (red-blue-shift)
        // then apply tonemapping
        sqrtToneMappingShader.use()
        sqrtToneMappingShader.v1("ySign", if(flipY) -1f else 1f)
        val colorDepth = DefaultConfig["display.colorDepth", 8]
        val minValue = 1f/(1 shl colorDepth)
        val rel = sqrt(w*h.toFloat())
        // artistic scale
        val caScale = 0.01f
        val ca = camera.chromaticAberration[cameraTime] * caScale
        val cao = camera.chromaticOffset[cameraTime] * caScale
        sqrtToneMappingShader.v1("chromaticAberration", ca)
        sqrtToneMappingShader.v2("chromaticOffset", cao)
        val fxScaleX = 1f*w/rel
        val fxScaleY = 1f*h/rel

        // avg brightness: exp avg(log (luminance + offset4black)) (Reinhard tone mapping paper)
        // middle gray = 0.18?

        val dst = camera.distortion[cameraTime]
        val dstOffset = camera.distortionOffset[cameraTime]
        sqrtToneMappingShader.v3("fxScale", fxScaleX, fxScaleY, 1f+dst.z)
        sqrtToneMappingShader.v2("distortion", dst.x, dst.y)
        sqrtToneMappingShader.v2("distortionOffset", dstOffset)
        sqrtToneMappingShader.v1("minValue", minValue)
        sqrtToneMappingShader.v1("toneMapper", when(camera.toneMapping){
            ToneMappers.RAW -> 0f
            ToneMappers.REINHARD -> 1f
            ToneMappers.ACES -> 2f
            ToneMappers.UCHIMURA -> 3f
        })
        flat01.draw(sqrtToneMappingShader)
        GFX.check()

        /**
         * apply the LUT for sepia looks, cold looks, general color correction, ...
         * uses the Unreal Engine "format" of an 256x16 image (or 1024x32)
         * */

        if(useLUT){
            bindTarget()
            buffer.bindTextures(0, true)
            lutShader.use()
            lut!!.bind(1, false)
            lut.clamping(false)
            flat01.draw(lutShader)
            GFX.check()
        }

        FBStack.clear(w, h, true)
        FBStack.clear(w, h, false)

        glEnable(GL_BLEND)

    }

}