package me.anno.studio.rems

import me.anno.image.ImageGPUCache.getLUT
import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.RenderState
import me.anno.gpu.RenderState.blendMode
import me.anno.gpu.RenderState.depthMode
import me.anno.gpu.RenderState.renderDefault
import me.anno.gpu.RenderState.renderPurely
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.ShaderLib.ascColorDecisionList
import me.anno.gpu.ShaderLib.brightness
import me.anno.gpu.ShaderLib.createShader
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture3D
import me.anno.objects.Camera
import me.anno.objects.Camera.Companion.DEFAULT_VIGNETTE_STRENGTH
import me.anno.objects.Transform
import me.anno.objects.Transform.Companion.drawUICircle
import me.anno.objects.Transform.Companion.xAxis
import me.anno.objects.effects.GaussianBlur
import me.anno.objects.effects.ToneMappers
import me.anno.studio.rems.RemsStudio.gfxSettings
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.editor.sceneView.Gizmo.drawGizmo
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.editor.sceneView.ISceneView
import me.anno.utils.types.Vectors.is000
import me.anno.utils.types.Vectors.is1111
import me.anno.utils.types.Vectors.minus
import me.anno.utils.types.Vectors.times
import me.anno.video.MissingFrameException
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*
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
    // (Oh The Humanity! - Kate Gregory [C++ on Sea 2019])

    lateinit var sqrtToneMappingShader: BaseShader
    lateinit var lutShader: BaseShader
    lateinit var addBloomShader: BaseShader

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

    private var isInited = false
    private fun init() {

        // add randomness against banding

        sqrtToneMappingShader = BaseShader("sqrt/tone-mapping",
            "" +
                    "in vec2 attr0;\n" +
                    "uniform float ySign;\n" +
                    "void main(){" +
                    "   vec2 coords = attr0*2.0-1.0;\n" +
                    "   gl_Position = vec4(coords.x, coords.y * ySign, 0.0, 1.0);\n" +
                    "   uv = attr0;\n" +
                    "}", listOf(Variable("vec2","uv")), "" +
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
                                "\t\tcase ${it.id}: toneMapped = ${it.glslFuncName}(raw);break;\n"
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
                    "}", listOf(Variable("vec2","uv")), "" +
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

        addBloomShader = createShader(
            "addBloom", "" +
                    "in vec2 attr0;\n" +
                    "void main(){" +
                    "   gl_Position = vec4(attr0*2.0-1.0, 0.0, 1.0);\n" +
                    "   uv = attr0;\n" +
                    "}", listOf(Variable("vec2","uv")), "" +
                    "uniform sampler2D original, blurred;\n" +
                    "uniform float intensity;\n" +
                    "void main(){" +
                    "   gl_FragColor = texture(original, uv) + intensity * texture(blurred, uv);\n" +
                    "   gl_FragColor.a = clamp(gl_FragColor.a, 0.0, 1.0);\n" +
                    "}", listOf("original", "blurred")
        )


        isInited = true
    }

    fun getNextBuffer(
        name: String,
        previous: Framebuffer,
        offset: Int,
        nearest: GPUFiltering,
        samples: Int?
    ): Framebuffer {
        val next = FBStack[name, previous.w, previous.h, 4, usesFPBuffers, samples ?: previous.samples]
        previous.bindTextures(offset, nearest, Clamping.CLAMP)
        return next
    }

    var lastCameraTransform = Matrix4f()
    var lastGlobalCameraTransform = Matrix4f()
    var lGCTInverted = Matrix4f()
    var usesFPBuffers = false

    val mayUseMSAA get() = if (isFinalRendering) DefaultConfig["rendering.useMSAA", true] else DefaultConfig["ui.editor.useMSAA", gfxSettings["ui.editor.useMSAA"]]

    // rendering must be done in sync with the rendering thread (OpenGL limitation) anyways, so one object is enough
    val stack = Matrix4fArrayList()
    fun draw(
        camera: Camera, scene: Transform, x: Int, y: Int, w: Int, h: Int, time: Double,
        flipY: Boolean, renderer: Renderer, sceneView: ISceneView?
    ) {

        GFX.currentCamera = camera

        useFrame(renderer) {

            usesFPBuffers = sceneView?.usesFPBuffers ?: camera.toneMapping != ToneMappers.RAW8

            val isFakeColorRendering = renderer.isFakeColor

            stack.clear()
            drawScene(scene, camera, time, x, y, w, h, flipY, isFakeColorRendering, sceneView)

        }


    }

    fun clearColors(
        camera: Camera, buffer: Framebuffer?, needsTemporaryBuffer: Boolean,
        x: Int, y: Int, w: Int, h: Int
    ) {

        Frame.bind()

        glClearColor(0f, 0f, 0f, 1f)

        if (camera.useDepth) {
            if (buffer != null) {
                if (needsTemporaryBuffer) {
                    glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)
                } else {
                    drawRect(x, y, w, h, black)
                    glClear(GL_DEPTH_BUFFER_BIT)
                }
            } else {
                glClear(GL_DEPTH_BUFFER_BIT)
            }
        } else {
            if (buffer != null) {
                glClear(GL_COLOR_BUFFER_BIT)
            }
        }

        if (buffer == null) {
            GFX.check()
            drawRect(x, y, w, h, black)
        }

    }

    fun drawScene(
        scene: Transform, camera: Camera,
        time: Double,
        x0: Int, y0: Int, w: Int, h: Int, flipY: Boolean,
        isFakeColorRendering: Boolean, sceneView: ISceneView?
    ) {

        stack.identity()

        val mayUseMSAA = mayUseMSAA
        val samples = if (mayUseMSAA && !isFakeColorRendering) 8 else 1

        GFX.check()

        if (!isInited) init()

        RemsStudio.currentlyDrawnCamera = camera

        val (cameraTransform, cameraTime) = camera.getGlobalTransformTime(time)
        lastGlobalCameraTransform.set(cameraTransform)
        lGCTInverted.set(cameraTransform).invert()

        // todo optimize to use target directly, if no buffer in-between is required
        // (for low-performance devices)

        val distortion = camera.distortion[cameraTime]
        val vignetteStrength = camera.vignetteStrength[cameraTime]
        val chromaticAberration = camera.chromaticAberration[cameraTime]
        val toneMapping = camera.toneMapping

        val cgOffset = camera.cgOffsetAdd[cameraTime] - camera.cgOffsetSub[cameraTime]
        val cgSlope = camera.cgSlope[cameraTime]
        val cgPower = camera.cgPower[cameraTime]
        val cgSaturation = camera.cgSaturation[cameraTime]

        val bloomIntensity = camera.bloomIntensity[cameraTime]
        val bloomSize = camera.bloomSize[cameraTime]
        val bloomThreshold = camera.bloomThreshold[cameraTime]

        val needsCG = !cgOffset.is000() || !cgSlope.is1111() || !cgPower.is1111() || cgSaturation != 1f
        val needsBloom = bloomIntensity > 0f && bloomSize > 0f

        var needsTemporaryBuffer = !isFakeColorRendering
        if (needsTemporaryBuffer) {
            needsTemporaryBuffer = // issues are resolved: clipping was missing maybe...
                flipY ||
                        samples > 1 ||
                        !distortion.is000() ||
                        vignetteStrength > 0f ||
                        chromaticAberration > 0f ||
                        toneMapping != ToneMappers.RAW8 ||
                        needsCG || needsBloom ||
                        w > GFX.width || h > GFX.height
        }

        var buffer: Framebuffer? =
            if (needsTemporaryBuffer) FBStack["Scene-Main", w, h, 4, usesFPBuffers, samples]
            else RenderState.currentBuffer

        // LOGGER.info("$needsTemporaryBuffer ? $buffer")

        val x = if (needsTemporaryBuffer) 0 else x0
        val y = if (needsTemporaryBuffer) 0 else y0// GFX.height - (y0 + h)

        blendMode.use(if (isFakeColorRendering) null else BlendMode.DEFAULT) {
            depthMode.use(if(camera.useDepth) DepthMode.GREATER else DepthMode.ALWAYS) {

                useFrame(x, y, w, h, false, buffer) {

                    Frame.bind()

                    clearColors(camera, buffer, needsTemporaryBuffer, x, y, w, h)

                    // draw the 3D stuff
                    nearZ = camera.nearZ[cameraTime]
                    farZ = camera.farZ[cameraTime]

                    GFX.applyCameraTransform(
                        camera, cameraTime, cameraTransform,
                        stack
                    )

                    // val white = Vector4f(1f, 1f, 1f, 1f)
                    val white = Vector4f(camera.color[cameraTime])
                    val whiteMultiplier = camera.colorMultiplier[cameraTime]
                    val whiteAlpha = white.w
                    white.mul(whiteMultiplier)
                    white.w = whiteAlpha

                    // use different settings for white balance etc...
                    // remember the transform for later use
                    lastCameraTransform.set(stack)

                    if (!isFinalRendering && camera != nullCamera) {
                        stack.next { nullCamera?.draw(stack, time, white) }
                    }

                    if (!isFakeColorRendering && !isFinalRendering && sceneView != null) {
                        // todo why is this add not working???
                        // it is working for the scene components...
                        blendMode.use(BlendMode.ADD) {
                            drawGrid(cameraTransform, sceneView)
                        }
                    }

                    stack.next { scene.draw(stack, time, white) }

                    GFX.check()

                    if (!isFinalRendering && !isFakeColorRendering) {
                        drawGizmo(cameraTransform, x, y, w, h)
                        GFX.check()
                    }

                    drawSelectionRing(isFakeColorRendering, camera, time)

                }
            }

        }

        /*val enableCircularDOF = isKeyDown('o') && isKeyDown('f')
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
        val needsLUT = !isFakeColorRendering && lutFile.exists && !lutFile.isDirectory
        val lut = if (needsLUT) getLUT(lutFile, true, 20_000) else null

        if (lut == null && needsLUT && isFinalRendering) throw MissingFrameException(lutFile)

        if (buffer != null && needsTemporaryBuffer) {

            renderPurely {

                if (needsBloom) {
                    buffer = applyBloom(buffer!!, w, h, bloomSize, bloomIntensity, bloomThreshold)
                }

                useFrame(x0, y0, w, h, false) {
                    if (lut != null) {
                        drawWithLUT(buffer!!, isFakeColorRendering, camera, cameraTime, w, h, flipY, lut)
                    } else {
                        drawWithoutLUT(buffer!!, isFakeColorRendering, camera, cameraTime, w, h, flipY)
                    }
                }

            }


        }

    }

    private fun drawWithLUT(
        buffer: Framebuffer,
        isFakeColorRendering: Boolean,
        camera: Camera,
        cameraTime: Double,
        w: Int,
        h: Int,
        flipY: Boolean,
        lut: Texture3D
    ) {

        val lutBuffer = getNextBuffer("Scene-LUT", buffer, 0, GPUFiltering.LINEAR, 1)
        useFrame(lutBuffer) {
            drawColors(isFakeColorRendering, camera, cameraTime, w, h, flipY)
        }

        /**
         * apply the LUT for sepia looks, cold looks, general color correction, ...
         * uses the Unreal Engine "format" of an 256x16 image (or 1024x32)
         * */
        val lutShader = lutShader.value
        lutShader.use()
        lut.bind(1, GPUFiltering.LINEAR)
        lutBuffer.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
        lut.clamping(false)
        flat01.draw(lutShader)
        GFX.check()

    }

    private fun drawWithoutLUT(
        buffer: Framebuffer,
        isFakeColorRendering: Boolean,
        camera: Camera,
        cameraTime: Double,
        w: Int,
        h: Int,
        flipY: Boolean
    ) {
        buffer.bindTextures(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
        drawColors(isFakeColorRendering, camera, cameraTime, w, h, flipY)
    }

    private fun applyBloom(
        buffer: Framebuffer, w: Int, h: Int,
        bloomSize: Float, bloomIntensity: Float, bloomThreshold: Float
    ): Framebuffer {

        // create blurred version
        GaussianBlur.draw(buffer, bloomSize, w, h, 1, bloomThreshold, false, Matrix4fArrayList())
        val bloomed = getNextBuffer("Scene-Bloom", buffer, 0, GPUFiltering.TRULY_NEAREST, 1)

        // add it on top
        useFrame(bloomed) {
            val shader = addBloomShader.value
            shader.use()
            shader.v1("intensity", bloomIntensity)
            flat01.draw(shader)
        }

        return bloomed

    }

    private fun drawColors(
        isFakeColorRendering: Boolean,
        camera: Camera, cameraTime: Double,
        w: Int, h: Int, flipY: Boolean
    ) {

        /**
         * Tone Mapping, Distortion, and applying the sqrt operation (reverse pow(, 2.2))
         * */

        // outputTexture.bind(0, true)

        // todo render at higher resolution for extreme distortion?
        // msaa should help, too
        // add camera pseudo effects (red-blue-shift)
        // then apply tonemapping
        val shader = sqrtToneMappingShader.value
        shader.use()
        shader.v1("ySign", if (flipY) -1f else 1f)
        val colorDepth = DefaultConfig["gpu.display.colorDepth", 8]

        val minValue = if (isFakeColorRendering) -1f else 1f / (1 shl colorDepth)
        shader.v1("minValue", minValue)

        uploadCameraUniforms(shader, isFakeColorRendering, camera, cameraTime, w, h)

        // draw it!
        flat01.draw(shader)
        GFX.check()

    }

    private fun uploadCameraUniforms(
        shader: Shader, isFakeColorRendering: Boolean,
        camera: Camera, cameraTime: Double,
        w: Int, h: Int
    ) {

        val distortion = camera.distortion[cameraTime]
        val distortionOffset = camera.distortionOffset[cameraTime]
        val vignetteStrength = camera.vignetteStrength[cameraTime]
        val chromaticAberration = camera.chromaticAberration[cameraTime]
        val toneMapping = camera.toneMapping

        val cgOffset = camera.cgOffsetAdd[cameraTime] - camera.cgOffsetSub[cameraTime]
        val cgSlope = camera.cgSlope[cameraTime]
        val cgPower = camera.cgPower[cameraTime]
        val cgSaturation = camera.cgSaturation[cameraTime]

        val rel = sqrt(w * h.toFloat())
        val fxScaleX = 1f * w / rel
        val fxScaleY = 1f * h / rel

        // artistic scale
        val chromaticScale = 0.01f
        if (!isFakeColorRendering) {
            val ca = chromaticAberration * chromaticScale
            val cao = camera.chromaticOffset[cameraTime] * chromaticScale
            val angle = (camera.chromaticAngle[cameraTime] * 2 * Math.PI).toFloat()
            shader.v2("chromaticAberration", cos(angle) * ca / fxScaleX, sin(angle) * ca / fxScaleY)
            shader.v2("chromaticOffset", cao)
        }

        // avg brightness: exp avg(log (luminance + offset4black)) (Reinhard tone mapping paper)
        // middle gray = 0.18?

        // distortion
        shader.v3("fxScale", fxScaleX, fxScaleY, 1f + distortion.z())
        shader.v2("distortion", distortion.x(), distortion.y())
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

    }

    private fun drawGrid(cameraTransform: Matrix4f, sceneView: ISceneView) {
        stack.next {
            if (sceneView.isLocked2D) {
                stack.rotate(Math.PI.toFloat() / 2, xAxis)
            }
            Grid.draw(stack, cameraTransform)
        }
    }

    private fun drawSelectionRing(isFakeColorRendering: Boolean, camera: Camera, time: Double) {
        /**
         * draw the selection ring for selected objects
         * draw it after everything else and without depth
         * */
        if (!isFinalRendering && !isFakeColorRendering && selectedTransform != camera) { // seeing the own camera is irritating xD
            val stack = stack
            selectedTransform?.apply {
                renderDefault {
                    val (transform, _) = getGlobalTransformTime(time)
                    stack.next {
                        stack.mul(transform)
                        stack.scale(0.02f)
                        drawUICircle(stack, 1f, 0.700f, Vector4f(1f, 0.9f, 0.5f, 1f))
                        stack.scale(1.2f)
                        drawUICircle(stack, 1f, 0.833f, Vector4f(0f, 0f, 0f, 1f))
                    }
                }
            }
        }
    }


}