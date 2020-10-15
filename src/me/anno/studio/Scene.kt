package me.anno.studio

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.ShaderLib.ascColorDecisionList
import me.anno.gpu.ShaderLib.brightness
import me.anno.gpu.ShaderLib.createShader
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.ClampMode
import me.anno.gpu.texture.NearestMode
import me.anno.objects.Camera
import me.anno.objects.Camera.Companion.DEFAULT_VIGNETTE_STRENGTH
import me.anno.objects.Transform.Companion.xAxis
import me.anno.objects.cache.Cache
import me.anno.objects.effects.ToneMappers
import me.anno.studio.RemsStudio.gfxSettings
import me.anno.studio.RemsStudio.nullCamera
import me.anno.studio.RemsStudio.selectedTransform
import me.anno.ui.editor.sceneView.Gizmo.drawGizmo
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.editor.sceneView.ISceneView
import me.anno.utils.is000
import me.anno.utils.is1111
import me.anno.utils.times
import me.anno.utils.warn
import me.anno.video.MissingFrameException
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL30
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Scene {

    var nearZ = 0.001f
    var farZ = 1000f

    // use a framebuffer, where we draw sq(color)
    // then we use a shader to draw sqrt(sq(color))
    // this should give correct color mixing <3
    // (color gamma correction, 2.2 is close to 2.0; shouldn't matter in my opinion)
    // can't remove the heart after this talk: https://www.youtube.com/watch?v=SzoquBerhUc ;)

    lateinit var sqrtToneMappingShader: Shader
    lateinit var lutShader: Shader

    private var isInited = false
    private fun init() {

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
                "   const float invWhiteSq = ${1.0 / 16.0};\n" +
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

        sqrtToneMappingShader = Shader("sqrt/tone-mapping",
            "" +
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
                    "uniform vec2 chromaticAberration;" +
                    "uniform vec2 chromaticOffset;\n" +
                    "uniform vec2 distortion, distortionOffset;\n" +
                    "uniform float vignetteStrength;\n" +
                    "uniform vec3 vignetteColor;\n" +
                    "uniform float minValue;\n" +
                    "uniform int toneMapper;\n" +
                    noiseFunc +
                    reinhardToneMapping +
                    acesToneMapping +
                    uchimuraToneMapping +
                    brightness +
                    ascColorDecisionList +
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
                    "}\n" +
                    "float softMin(float a, float b, float k){\n" +
                    "   return -(log(exp(k*-a)+exp(k*-b))/k);\n" +
                    "}" +
                    "void main(){" +
                    "   vec2 uv2 = (uv-0.5)*fxScale.z+0.5;\n" +
                    "   vec2 nuv = (uv2-0.5)*fxScale.xy;\n" +
                    "   vec2 duv = chromaticAberration * nuv + chromaticOffset;\n" +
                    "   vec2 uvG = distort(uv2, nuv, vec2(0.0));\n" +
                    "   vec4 col0 = getColor(uvG);\n" +
                    "   if(minValue < 0.0){" +
                    "       gl_FragColor = col0;\n" +
                    "   } else {" +
                    "       vec2 uvR = distort(uv2, nuv, duv), uvB = distort(uv2, nuv, -duv);\n" +
                    "       float r = getColor(uvR).r;\n" +
                    "       vec2 ga = col0.ga;\n" +
                    "       float b = getColor(uvB).b;\n" +
                    "       vec3 raw = vec3(r, ga.x, b);\n" +
                    "       vec3 toneMapped;\n" +
                    "       switch(toneMapper){\n" +
                    ToneMappers.values().joinToString("") {
                        "" +
                                "       case ${it.id}: toneMapped = ${it.glslFuncName}(raw);break;\n"
                    } +
                    "           default: toneMapped = vec3(1,0,1);\n" +
                    "       }" +
                    "       vec3 colorGraded = colorGrading(toneMapped);\n" +
                    // todo the grid is drawn with ^2 in raw8 mode, the rest is fine...
                    "       vec4 color = vec4(toneMapper == ${ToneMappers.RAW8.id} ? colorGraded : sqrt(colorGraded), ga.y);\n" +
                    "       float rSq = dot(nuv,nuv);\n" + // nuv nuv 😂 (hedgehog sounds for German children)
                    "       color = mix(vec4(vignetteColor, 1.0), color, 1.0/(1.0 + vignetteStrength*rSq));\n" +
                    "       gl_FragColor = color + random(uv) * minValue;\n" +
                    "   }" +
                    "}"
        )

        // todo Tone Mapping on Video objects for performance improvements (doesn't need fp framebuffer)

        lutShader = createShader(
            "lut", "" +
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
                    "}", listOf("tex", "lut")
        )

        isInited = true
    }

    fun getNextBuffer(
        name: String,
        previous: Framebuffer,
        offset: Int,
        nearest: NearestMode,
        samples: Int?
    ): Framebuffer {
        val next = FBStack[name, previous.w, previous.h, samples ?: previous.samples, usesFPBuffers]
        // next.bind()
        previous.bindTextures(offset, nearest, ClampMode.CLAMP)
        return next
    }

    var lastCameraTransform = Matrix4f()
    var lastGlobalCameraTransform = Matrix4f()
    var lGCTInverted = Matrix4f()
    var usesFPBuffers = false

    // rendering must be done in sync with the rendering thread (OpenGL limitation) anyways, so one object is enough
    val stack = Matrix4fArrayList()
    fun draw(
        camera: Camera, x0: Int, y0: Int, w: Int, h: Int, time: Double,
        flipY: Boolean, drawMode: ShaderPlus.DrawMode, sceneView: ISceneView?
    ) {

        GFX.ensureEmptyStack()

        GFX.drawMode = drawMode
        usesFPBuffers = sceneView?.usesFPBuffers ?: camera.toneMapping != ToneMappers.RAW8

        val isFakeColorRendering = when (drawMode) {
            ShaderPlus.DrawMode.COLOR, ShaderPlus.DrawMode.COLOR_SQUARED -> false
            else -> true
        }

        // we must have crashed before;
        // somewhere in this function
        if (stack.currentIndex > 0) {
            warn("Must have crashed inside Scene.draw() :/")
            // cleanup is done with stack.clear()
        }

        stack.clear()

        val mayUseMSAA = if (isFinalRendering)
            DefaultConfig["rendering.useMSAA", true]
        else
            DefaultConfig["ui.editor.useMSAA", gfxSettings["ui.editor.useMSAA"]]
        val samples = if (mayUseMSAA && !isFakeColorRendering) 8 else 1

        GFX.check()

        if (!isInited) init()

        RemsStudio.usedCamera = camera

        val (cameraTransform, cameraTime) = camera.getGlobalTransform(time)
        lastGlobalCameraTransform.set(cameraTransform)
        lGCTInverted.set(cameraTransform)
        lGCTInverted.invert()

        // todo optimize to use target directly, if no buffer in-between is required
        // (for low-performance devices)

        val distortion = camera.distortion[cameraTime]
        val distortionOffset = camera.distortionOffset[cameraTime]
        val vignetteStrength = camera.vignetteStrength[cameraTime]
        val chromaticAberration = camera.chromaticAberration[cameraTime]
        val toneMapping = camera.toneMapping

        val cgOffset = camera.cgOffset[cameraTime]
        val cgSlope = camera.cgSlope[cameraTime]
        val cgPower = camera.cgPower[cameraTime]
        val cgSaturation = camera.cgSaturation[cameraTime]

        var needsTemporaryBuffer = !isFakeColorRendering
        if (needsTemporaryBuffer) {
            needsTemporaryBuffer = true || // ^^
                    flipY ||
                    samples > 1 ||
                    !distortion.is000() ||
                    vignetteStrength > 0f ||
                    chromaticAberration > 0f ||
                    toneMapping != ToneMappers.RAW8 ||
                    !cgOffset.is000() ||
                    !cgSlope.is1111() ||
                    !cgPower.is1111() ||
                    cgSaturation != 1f
        }

        var buffer: Framebuffer? =
            if (needsTemporaryBuffer) FBStack["Scene-Main", w, h, samples, usesFPBuffers]
            else Frame.currentFrame!!.buffer

        Frame(0, 0, w, h, false, buffer) {

            Frame.currentFrame!!.bind()

            if (camera.useDepth) {
                GL30.glEnable(GL30.GL_DEPTH_TEST)
                GL30.glClearDepth(1.0)
                GL30.glDepthRange(-1.0, 1.0)
                GL30.glDepthFunc(GL30.GL_LESS)
                if (buffer != null) {
                    glClearColor(0f, 0f, 0f, 1f)
                    GL30.glClear(GL30.GL_DEPTH_BUFFER_BIT or GL30.GL_COLOR_BUFFER_BIT)
                } else {
                    GL30.glClear(GL30.GL_DEPTH_BUFFER_BIT)
                }
            } else {
                GL30.glDisable(GL30.GL_DEPTH_TEST)
                if (buffer != null) {
                    glClearColor(0f, 0f, 0f, 1f)
                    GL30.glClear(GL30.GL_COLOR_BUFFER_BIT)
                }
            }

            if (buffer == null) {
                GFX.check()
                GFX.drawRect(x0, y0, w, h, black)
            }

            // draw the 3D stuff
            nearZ = camera.nearZ[cameraTime]
            farZ = camera.farZ[cameraTime]

            GFX.applyCameraTransform(camera, cameraTime, cameraTransform, stack)

            // val white = Vector4f(1f, 1f, 1f, 1f)
            val white = Vector4f(camera.color[cameraTime])
            val whiteMultiplier = camera.colorMultiplier[cameraTime]
            white.x *= whiteMultiplier
            white.y *= whiteMultiplier
            white.z *= whiteMultiplier

            // use different settings for white balance etc...

            // remember the transform for later use
            lastCameraTransform.set(stack)

            if (!isFakeColorRendering && sceneView != null) {
                stack.pushMatrix()
                if (sceneView.isLocked2D) {
                    stack.rotate(Math.PI.toFloat() / 2, xAxis)
                }
                Grid.draw(stack, cameraTransform)
                stack.popMatrix()
            }

            val bd = BlendDepth(if (isFakeColorRendering) null else BlendMode.DEFAULT, camera.useDepth)
            bd.bind()

            GL30.glDepthMask(true)

            if (!isFinalRendering && camera != nullCamera) {
                stack.pushMatrix()
                nullCamera.draw(stack, time, white)
                stack.popMatrix()
            }

            stack.pushMatrix()
            RemsStudio.root.draw(stack, time, white)
            stack.popMatrix()

            GFX.check()

            if (!isFinalRendering && !isFakeColorRendering) {
                drawGizmo(cameraTransform, x0, y0, w, h)
                GFX.check()
            }

            /**
             * draw the selection ring for selected objects
             * draw it after everything else and without depth
             * */
            if (!isFinalRendering && !isFakeColorRendering && selectedTransform != camera) { // seeing the own camera is irritating xD
                selectedTransform?.apply {
                    val bd2 = BlendDepth(BlendMode.DEFAULT, false)
                    bd2.bind()
                    val (transform, _) = getGlobalTransform(time)
                    stack.pushMatrix()
                    stack.mul(transform)
                    stack.scale(0.02f)
                    drawUICircle(stack, 1f, 0.700f, Vector4f(1f, 0.9f, 0.5f, 1f))
                    stack.scale(1.2f)
                    drawUICircle(stack, 1f, 0.833f, Vector4f(0f, 0f, 0f, 1f))
                    stack.popMatrix()
                    bd2.unbind()
                }
            }

            bd.unbind()

        }

        val bd = BlendDepth(null, false)
        bd.bind()

        /*val enableCircularDOF = 'O'.toInt() in keysDown && 'F'.toInt() in keysDown
        if(enableCircularDOF){
            // todo render dof instead of bokeh blur only
            // make bokeh blur an additional camera effect?
            val srcBuffer = buffer.msBuffer ?: buffer
            srcBuffer.ensure()
            val src = srcBuffer.textures[0]
            buffer = getNextBuffer("Scene-Bokeh", buffer, 0, true, samples = 1)
            Frame(buffer){
                BokehBlur.draw(src, Framebuffer.stack.peek()!!, 0.02f)
            }
        }*/

        val lutFile = camera.lut
        val needsLUT = !isFakeColorRendering && lutFile.exists() && !lutFile.isDirectory
        val lut = if (needsLUT) Cache.getLUT(lutFile, true, 20_000) else null

        if (lut == null && needsLUT && isFinalRendering) throw MissingFrameException(File(""))

        fun drawColors() {

            /**
             * Tone Mapping, Distortion, and applying the sqrt operation (reverse pow(, 2.2))
             * */

            // outputTexture.bind(0, true)

            // todo render at higher resolution for extreme distortion?
            // msaa should help, too
            // add camera pseudo effects (red-blue-shift)
            // then apply tonemapping
            val shader = sqrtToneMappingShader
            shader.use()
            shader.v1("ySign", if (flipY) -1f else 1f)
            val colorDepth = DefaultConfig["gpu.display.colorDepth", 8]

            val minValue = if (isFakeColorRendering) -1f else 1f / (1 shl colorDepth)
            shader.v1("minValue", minValue)

            val rel = sqrt(w * h.toFloat())
            val fxScaleX = 1f * w / rel
            val fxScaleY = 1f * h / rel

            // artistic scale
            val caScale = 0.01f
            if (!isFakeColorRendering) {
                val ca = chromaticAberration * caScale
                val cao = camera.chromaticOffset[cameraTime] * caScale
                val angle = (camera.chromaticAngle[cameraTime] * 2 * Math.PI).toFloat()
                shader.v2("chromaticAberration", cos(angle) * ca / fxScaleX, sin(angle) * ca / fxScaleY)
                shader.v2("chromaticOffset", cao)
            }

            // avg brightness: exp avg(log (luminance + offset4black)) (Reinhard tone mapping paper)
            // middle gray = 0.18?

            // distortion
            shader.v3("fxScale", fxScaleX, fxScaleY, 1f + distortion.z)
            shader.v2("distortion", distortion.x, distortion.y)
            shader.v2("distortionOffset", distortionOffset)
            if (!isFakeColorRendering) {
                // vignette
                shader.v1("vignetteStrength", DEFAULT_VIGNETTE_STRENGTH * vignetteStrength)
                shader.v3("vignetteColor", camera.vignetteColor[cameraTime])
                // randomness against banding
                // tone mapping
                shader.v1("toneMapper", toneMapping.id)
                // color grading
                shader.v3("cgOffset", cgOffset)
                shader.v3X("cgSlope", cgSlope)
                shader.v3X("cgPower", cgPower)
                shader.v1("cgSaturation", cgSaturation)
            }
            // draw it!
            flat01.draw(shader)
            GFX.check()

        }

        if (buffer != null) {

            val useLUT = lut != null
            if (useLUT) {

                buffer = getNextBuffer("Scene-LUT", buffer, 0, nearest = NearestMode.LINEAR, samples = 1)
                Frame(buffer) {
                    drawColors()
                }

                /**
                 * apply the LUT for sepia looks, cold looks, general color correction, ...
                 * uses the Unreal Engine "format" of an 256x16 image (or 1024x32)
                 * */
                buffer.bindTextures(0, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)
                lutShader.use()
                lut!!.bind(1, NearestMode.LINEAR)
                lut.clamping(false)
                flat01.draw(lutShader)
                GFX.check()

            } else {

                buffer.bindTextures(0, NearestMode.TRULY_NEAREST, ClampMode.CLAMP)
                drawColors()

            }

        }

        bd.unbind()

        if (stack.currentIndex != 0) {
            warn("Some function isn't correctly pushing/popping the stack!")
            // further analysis by testing each object individually?
        }

        GFX.drawMode = ShaderPlus.DrawMode.COLOR_SQUARED

    }


}