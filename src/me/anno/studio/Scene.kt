package me.anno.studio

import me.anno.config.DefaultConfig
import me.anno.config.DefaultStyle.black
import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib.createShader
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFX.isFakeColorRendering
import me.anno.gpu.GFX.isFinalRendering
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.texture.ClampMode
import me.anno.input.Input.keysDown
import me.anno.objects.Camera
import me.anno.objects.Camera.Companion.DEFAULT_VIGNETTE_STRENGTH
import me.anno.objects.Transform.Companion.xAxis
import me.anno.objects.Transform.Companion.yAxis
import me.anno.objects.Transform.Companion.zAxis
import me.anno.gpu.blending.BlendMode
import me.anno.objects.cache.Cache
import me.anno.objects.effects.BokehBlur
import me.anno.objects.effects.ToneMappers
import me.anno.studio.Studio.selectedTransform
import me.anno.ui.editor.sceneView.Grid
import me.anno.ui.editor.sceneView.Grid.drawLine01
import me.anno.ui.editor.sceneView.ISceneView
import me.anno.ui.editor.sceneView.SceneView
import me.anno.utils.times
import me.anno.utils.warn
import me.anno.video.MissingFrameException
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL30
import java.io.File
import kotlin.math.sqrt

object Scene {

    private val LOGGER = LogManager.getLogger(Scene::class)

    var nearZ = 0.001f
    var farZ = 1000f

    // use a framebuffer, where we draw sq(color)
    // then we use a shader to draw sqrt(sq(color))
    // this should give correct color mixing <3
    // (color gamma correction, 2.2 is close to 2.0; shouldn't matter in my opinion)
    // can't remove the heart after this talk: https://www.youtube.com/watch?v=SzoquBerhUc ;)

    lateinit var sqrtToneMappingShader: Shader
    lateinit var lutShader: Shader
    lateinit var copyShader: Shader

    private var isInited = false
    private fun init(){

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
                    "uniform float chromaticAberration;" +
                    "uniform vec2 chromaticOffset;\n" +
                    "uniform vec2 distortion, distortionOffset;\n" +
                    "uniform float vignetteStrength;\n" +
                    "uniform vec4 vignetteColor;\n" +
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
                    "}\n" +
                    "float softMin(float a, float b, float k){\n" +
                    "   return -(log(exp(k*-a)+exp(k*-b))/k);\n" +
                    "}" +
                    // todo add vignette effect:
                    // todo mix with second color, depending on radius
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
                    "   float rSq = dot(nuv,nuv);\n" + // nuv nuv 😂 (hedgehog sounds for German children)
                    "   color = mix(vignetteColor, color, 1.0/(1.0 + vignetteStrength*rSq));\n" +
                    "   gl_FragColor = color + random(uv) * minValue;\n" +
                    "}"
        )

        lutShader = createShader("lut","" +
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

        copyShader = createShader("copy", "in vec2 attr0;\n" +
                "void main(){\n" +
                "   gl_Position = vec4(attr0*2.0-1.0, 0.5, 1.0);\n" +
                "   uv = attr0;\n" +
                "}\n", "varying vec2 uv;\n", "" +
                "uniform sampler2D tex;\n" +
                "void main(){" +
                "   gl_FragColor = texture(tex, uv);\n" +
                "}", listOf("tex"))

        isInited = true
    }

    fun getNextBuffer(name: String, buffer: Framebuffer, offset: Int, nearest: Boolean, withMultisampling: Boolean?): Framebuffer {
        val next = FBStack[name, buffer.w, buffer.h, withMultisampling ?: buffer.withMultisampling]
        next.bind()
        buffer.bindTextures(offset, nearest, ClampMode.CLAMP)
        return next
    }

    var lastCameraTransform = Matrix4f()
    var lastGlobalCameraTransform = Matrix4f()
    var lGCTInverted = Matrix4f()

    // rendering must be done in sync with the rendering thread (OpenGL limitation) anyways, so one object is enough
    val stack = Matrix4fArrayList()
    fun draw(target: Framebuffer?, camera: Camera, x0: Int, y0: Int, w: Int, h: Int, time: Double,
             flipY: Boolean, drawMode: ShaderPlus.DrawMode, sceneView: ISceneView?){

        GFX.ensureEmptyStack()

        GFX.drawMode = drawMode
        val isFakeColorRendering = isFakeColorRendering

        // we must have crashed before;
        // somewhere in this function
        if(stack.currentIndex > 0){
            warn("Must have crashed inside Scene.draw() :/")
            // cleanup is done with stack.clear()
        }

        stack.clear()

        val mayUseMSAA = if(isFinalRendering)
            DefaultConfig["rendering.useMSAA", true]
        else
            DefaultConfig["editor.useMSAA", true]
        val withMultisampling = mayUseMSAA && !isFakeColorRendering

        GFX.check()

        if(!isInited) init()

        Studio.usedCamera = camera

        val (cameraTransform, cameraTime) = camera.getGlobalTransform(time)
        lastGlobalCameraTransform.set(cameraTransform)
        lGCTInverted.set(cameraTransform)
        lGCTInverted.invert()

        GFX.clip(x0, y0, w, h)

        var buffer = FBStack["Scene-Main", w, h, withMultisampling]
        buffer.bind()
        //framebuffer.bind(w, h)

        GFX.check()
        GFX.drawRect(x0, y0, w, h, black)

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
        nearZ = camera.nearZ[cameraTime]
        farZ = camera.farZ[cameraTime]

        GFX.applyCameraTransform(camera, cameraTime, cameraTransform, stack)

        val white = Vector4f(1f, 1f, 1f, 1f)
        // camera.color[cameraTime]
        // use different settings for white balance etc...

        // remember the transform for later use
        lastCameraTransform.set(stack)

        if(!isFakeColorRendering && sceneView != null){
            stack.pushMatrix()
            if(sceneView.isLocked2D){
                stack.rotate(Math.PI.toFloat()/2, xAxis)
            }
            Grid.draw(stack, cameraTransform)
            stack.popMatrix()
        }

        var bd = BlendDepth(if(isFakeColorRendering) null else BlendMode.DEFAULT, camera.useDepth)
        bd.bind()

        GL30.glDepthMask(true)

        stack.pushMatrix()
        // root.draw(stack, editorHoverTime, Vector4f(1f,1f,1f,1f))
        Studio.nullCamera.draw(stack, time, white)
        stack.popMatrix()
        stack.pushMatrix()
        Studio.root.draw(stack, time, white)
        stack.popMatrix()

        GFX.check()
        displayGizmo(cameraTransform, x0, y0, w, h)
        GFX.check()

        /**
         * draw the selection ring for selected objects
         * draw it after everything else and without depth
         * */
        if(!isFinalRendering && !isFakeColorRendering && selectedTransform != camera){ // seeing the own camera is irritating xD
            selectedTransform?.apply {
                val bd2 = BlendDepth(BlendMode.DEFAULT, false)
                bd2.bind()
                val (transform, _) = getGlobalTransform(time)
                stack.pushMatrix()
                stack.mul(transform)
                stack.scale(0.02f)
                drawUICircle(stack, 1f, 0.7f, Vector4f(1f, 0.9f, 0.5f, 1f))
                stack.scale(1.2f)
                drawUICircle(stack, 1f, 0.833f, Vector4f(0f, 0f, 0f, 1f))
                stack.popMatrix()
                bd2.unbind()
            }
        }

        bd.unbind()
        bd = BlendDepth(null, false)
        bd.bind()

        val enableCircularDOF = 'O'.toInt() in keysDown && 'F'.toInt() in keysDown
        if(enableCircularDOF){
            // todo render dof instead of bokeh blur only
            // make bokeh blur an additional camera effect?
            val srcBuffer = buffer.msBuffer ?: buffer
            srcBuffer.ensure()
            val src = srcBuffer.textures[0]
            buffer = getNextBuffer("Scene-Bokeh", buffer, 0, true, withMultisampling = false)
            BokehBlur.draw(src, Framebuffer.stack.peek()!!, 0.02f)
        } else if(withMultisampling){
            // todo skip this step, and just use the correct buffer...
            // this is just a computationally not that expensive workaround
            val msBuffer = buffer.msBuffer!!
            msBuffer.bind()
            buffer = getNextBuffer("Scene-tmp0", buffer, 0, true, withMultisampling = false) // this is somehow not enough (why??)
            buffer = getNextBuffer("Scene-tmp1", buffer, 0, true, withMultisampling = false)
            val shader = copyShader
            msBuffer.bindTextures(0, false, ClampMode.CLAMP)
            shader.use()
            flat01.draw(shader)
        }

        fun bindTarget(){
            if(target == null){
                Framebuffer.bindNull()
                GFX.clip(x0, y0, w, h)
            } else {
                target.bind()
            }
        }

        val lutFile = camera.lut
        val needsLUT = !isFakeColorRendering && lutFile.exists() && !lutFile.isDirectory
        val lut = if(needsLUT) Cache.getLUT(lutFile, true, 20_000) else null

        // ("lut: $lutFile $lut")

        if(lut == null && needsLUT && isFinalRendering) throw MissingFrameException(File(""))

        val useLUT = lut != null
        if(useLUT){
            buffer = getNextBuffer("Scene-LT", buffer, 0, nearest = false, withMultisampling = false)
        } else {
            bindTarget()
            buffer.bindTextures(0, false, ClampMode.CLAMP)
        }

        /**
         * Tone Mapping, Distortion, and applying the sqrt operation (reverse pow(, 2.2))
         * */

        // outputTexture.bind(0, true)

        // todo render at higher resolution for extreme distortion?
        // msaa should help, too
        // add camera pseudo effects (red-blue-shift)
        // then apply tonemapping
        sqrtToneMappingShader.use()
        sqrtToneMappingShader.v1("ySign", if(flipY) -1f else 1f)
        val colorDepth = DefaultConfig["display.colorDepth", 8]
        val minValue = if(isFakeColorRendering) 0f else 1f/(1 shl colorDepth)
        val rel = sqrt(w*h.toFloat())
        // artistic scale
        val caScale = 0.01f
        if(isFakeColorRendering){// colors must be preserved for ids
            sqrtToneMappingShader.v1("chromaticAberration", 0f)
            sqrtToneMappingShader.v2("chromaticOffset", 0f, 0f)
        } else {
            val ca = camera.chromaticAberration[cameraTime] * caScale
            val cao = camera.chromaticOffset[cameraTime] * caScale
            sqrtToneMappingShader.v1("chromaticAberration", ca)
            sqrtToneMappingShader.v2("chromaticOffset", cao)
        }
        val fxScaleX = 1f*w/rel
        val fxScaleY = 1f*h/rel

        // avg brightness: exp avg(log (luminance + offset4black)) (Reinhard tone mapping paper)
        // middle gray = 0.18?

        val dst = camera.distortion[cameraTime]
        val dstOffset = camera.distortionOffset[cameraTime]
        sqrtToneMappingShader.v3("fxScale", fxScaleX, fxScaleY, 1f+dst.z)
        sqrtToneMappingShader.v2("distortion", dst.x, dst.y)
        sqrtToneMappingShader.v2("distortionOffset", dstOffset)
        sqrtToneMappingShader.v4("vignetteColor", camera.vignetteColor[cameraTime])
        val vignette = camera.vignetteStrength[cameraTime]
        sqrtToneMappingShader.v1("vignetteStrength", DEFAULT_VIGNETTE_STRENGTH * vignette)
        sqrtToneMappingShader.v1("minValue", minValue)
        sqrtToneMappingShader.v1("toneMapper", when(if(isFakeColorRendering) ToneMappers.RAW else camera.toneMapping){
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
            buffer.bindTextures(0, true, ClampMode.CLAMP)
            lutShader.use()
            lut!!.bind(1, false)
            lut.clamping(false)
            flat01.draw(lutShader)
            GFX.check()
        }

        FBStack.clear(w, h, true)
        FBStack.clear(w, h, false)

        // testModelRendering()

        bd.unbind()

        if(stack.currentIndex != 0){
            warn("Some function isn't correctly pushing/popping the stack!")
            // further analysis by testing each object individually?
        }

        GFX.drawMode = ShaderPlus.DrawMode.COLOR_SQUARED

        // todo could be moved to ensure empty stack...
        GFX.clearStack()

    }

    fun displayGizmo(cameraTransform: Matrix4f, x0: Int, y0: Int, w: Int, h: Int){

        /**
         * display a 3D gizmo
         * todo beautify a little, take inspiration from Blender maybe ;)
         * */

        val vx = cameraTransform.transformDirection(xAxis, Vector3f())
        val vy = cameraTransform.transformDirection(yAxis, Vector3f())
        val vz = cameraTransform.transformDirection(zAxis, Vector3f())

        val gizmoSize = 50f
        val gizmoPadding = 10f
        val gx = x0 + w - gizmoSize - gizmoPadding
        val gy = y0 + gizmoSize + gizmoPadding

        fun drawCircle(x: Float, y: Float, z: Float, color: Int){
            val lx = gx-x0
            val ly = gy-y0
            drawLine01(
                lx, ly, lx + gizmoSize * x, ly - gizmoSize * y,
                w, h, color, 1f)
            val rectSize = 7f - z * 3f
            GFX.drawRect(
                gx + gizmoSize * x - rectSize * 0.5f,
                gy - gizmoSize * y - rectSize * 0.5f,
                rectSize, rectSize, color or black)
        }

        tmpDistances[0] = vx.z
        tmpDistances[1] = vy.z
        tmpDistances[2] = vz.z

        tmpDistances.sortDescending()

        for(d in tmpDistances){
            if(d == vx.z) drawCircle(vx.x, vx.y, vx.z, 0xff7777)
            if(d == vy.z) drawCircle(vy.x, vy.y, vy.z, 0x77ff77)
            if(d == vz.z) drawCircle(vz.x, vz.y, vz.z, 0x7777ff)
        }

    }

    // avoid unnecessary allocations ;)
    private val tmpDistances = FloatArray(3)

}