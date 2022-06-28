package me.anno.ecs.components.shaders.effects

import me.anno.engine.ui.render.Renderers.toneMapping
import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.OpenGL.useFrame
import me.anno.gpu.copying.FramebufferToMemory
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.image.ImageGPUCache
import me.anno.io.ResourceHelper
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS

object FSR {

    private val vertex = ShaderLib.simpleVertexShader

    val code = lazy {
        val defines = ResourceHelper.loadText("shader/fsr2/ffx_a.h")
        val functions = ResourceHelper.loadText("shader/fsr2/ffx_fsr1.h")
        defines to functions
    }

    private val upscaleShader = lazy {

        val defines = code.value.first
        val functions = code.value.second

        val shader = Shader(
            "upscale", vertex, uvList, "" +
                    "uniform vec2 dstWH;\n" +
                    "uniform vec4 background;\n" +
                    "uniform sampler2D source;\n" +
                    "#define A_GPU 1\n" +
                    "#define A_GLSL 1\n" +
                    "#define ANNO 1\n" + // we use our custom version
                    defines +
                    "#define FSR_EASU_F 1\n" +
                    "vec4 FsrEasuAF(vec2 p){ return textureGather(source,p,3); }\n" +
                    "vec4 FsrEasuRF(vec2 p, vec4 alpha){ return mix(background.rrrr, textureGather(source,p,0), alpha); }\n" +
                    "vec4 FsrEasuGF(vec2 p, vec4 alpha){ return mix(background.gggg, textureGather(source,p,1), alpha); }\n" +
                    "vec4 FsrEasuBF(vec2 p, vec4 alpha){ return mix(background.bbbb, textureGather(source,p,2), alpha); }\n" +
                    functions +
                    "layout(location=0) out vec4 glFragColor;\n" +
                    "uniform vec4 con0,con1,con2,con3;\n" +
                    "uniform vec2 texelOffset;\n" +
                    "uniform bool applyToneMapping;\n" +
                    noiseFunc + // needed for tone mapping
                    toneMapping +
                    "void main(){\n" +
                    "   vec3 color;\n" +
                    "   float alpha = texture(source,uv).a;\n" +
                    "   vec2 coords = uv * dstWH;\n" +
                    "   FsrEasuF(color, coords, con0, con1, con2, con3);\n" +
                    "   glFragColor = vec4(applyToneMapping ? toneMapping(color) : color, alpha > .01 ? 1.0 : 0.0);\n" +
                    "}"
        )
        shader.glslVersion = 420 // for int->float->int ops, which are used for fast sqrt and such
        shader
    }

    private val sharpenShader = lazy {

        val defines = code.value.first
        val functions = code.value.second

        val shader = Shader(
            "upscale", vertex, uvList, "" +
                    "out vec4 glFragColor;\n" +
                    "uniform vec2 dstWH;\n" +
                    "uniform float sharpness;\n" +
                    "uniform sampler2D source;\n" +
                    "#define A_GPU 1\n" +
                    "#define A_GLSL 1\n" +
                    "#define FSR_RCAS_PASSTHROUGH_ALPHA 1\n" +
                    "#define ANNO 1\n" +
                    defines +
                    "#define FSR_RCAS_F 1\n" +
                    "vec4 FsrRcasLoadF(ivec2 p){ return texelFetch(source,p,0); }\n" +
                    // optional input transform
                    "void FsrRcasInputF(inout AF1 r,inout AF1 g,inout AF1 b){}\n" +
                    functions +
                    "void main(){\n" +
                    "   vec4 color;\n" +
                    "   ivec2 coords = ivec2(uv*dstWH);\n" +
                    "   FsrRcasF(color.r,color.g,color.b,color.a,coords,sharpness);\n" +
                    "   glFragColor = color;\n" +
                    "}"
        )
        shader.glslVersion = 420 // for int->float->int ops, which are used for fast sqrt and such
        shader.use()
        shader

    }

    fun upscale(
        sw: Int, sh: Int, x: Int, y: Int, w: Int, h: Int,
        flipY: Boolean, applyToneMapping: Boolean
    ) {
        // if source is null, the texture needs to be bound to slot 0
        val shader = upscaleShader.value
        shader.use()
        fsrConfig(shader, sw, sh, w, h)
        tiling(shader, flipY)
        texelOffset(shader, w, h)
        posSize(shader, x, y, w, h)
        shader.v4f("background", 0)
        shader.v1b("applyToneMapping", applyToneMapping)
        flat01.draw(shader)
    }

    fun upscale(
        sw: Int, sh: Int, x: Int, y: Int, w: Int, h: Int,
        flipY: Boolean, backgroundColor: Int, applyToneMapping: Boolean
    ) {
        // if source is null, the texture needs to be bound to slot 0
        val shader = upscaleShader.value
        shader.use()
        fsrConfig(shader, sw, sh, w, h)
        tiling(shader, flipY)
        texelOffset(shader, w, h)
        posSize(shader, x, y, w, h)
        shader.v4f("background", backgroundColor or (255 shl 24))
        shader.v1b("applyToneMapping", applyToneMapping)
        flat01.draw(shader)
    }

    private fun fsrConfig(shader: Shader, iw: Int, ih: Int, ow: Int, oh: Int) {
        shader.v4f("con0", iw.toFloat() / ow, ih.toFloat() / oh, 0.5f * iw / ow - 0.5f, 0.5f * ih / oh - 0.5f)
        shader.v4f("con1", 1f / iw, 1f / ih, 1f / iw, -1f / ih)
        shader.v4f("con2", -1f / iw, 2f / ih, 1f / iw, 2f / ih)
        shader.v4f("con3", 0f, 4f / ih, 0f, 0f)
    }

    fun upscale(source: ITexture2D, x: Int, y: Int, w: Int, h: Int, flipY: Boolean, applyToneMapping: Boolean) {
        source.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
        upscale(source.w, source.h, x, y, w, h, flipY, applyToneMapping)
    }

    fun upscale(
        source: ITexture2D, x: Int, y: Int, w: Int, h: Int,
        flipY: Boolean, backgroundColor: Int, applyToneMapping: Boolean
    ) {
        source.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
        upscale(source.w, source.h, x, y, w, h, flipY, backgroundColor, applyToneMapping)
    }

    fun sharpen(sharpness: Float, x: Int, y: Int, w: Int, h: Int, flipY: Boolean) {
        val shader = sharpenShader.value
        shader.use()
        shader.v1f("sharpness", sharpness)
        texelOffset(shader, w, h)
        tiling(shader, flipY)
        posSize(shader, x, y, w, h)
        flat01.draw(shader)
    }

    fun tiling(shader: Shader, flipY: Boolean) {
        shader.v4f("tiling", 1f, if (flipY) -1f else +1f, 0f, 0f)
    }

    fun sharpen(sharpness: Float, flipY: Boolean) {
        val shader = sharpenShader.value
        shader.use()
        shader.v1f("sharpness", sharpness)
        texelOffset(shader, GFX.viewportY, GFX.viewportHeight)
        tiling(shader, flipY)
        posSize(shader, 0f, 0f, 1f, 1f)
        flat01.draw(shader)
    }

    fun texelOffset(shader: Shader, w: Int, h: Int) {
        shader.v2f("dstWH", w.toFloat(), h.toFloat())
    }

    fun sharpen(source: ITexture2D, sharpness: Float, x: Int, y: Int, w: Int, h: Int, flipY: Boolean) {
        source.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
        sharpen(sharpness, x, y, w, h, flipY)
    }

    fun sharpen(source: ITexture2D, sharpness: Float, flipY: Boolean) {
        source.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
        sharpen(sharpness, flipY)
    }

    @JvmStatic
    fun main(args: Array<String>) {

        // testing to upscale and sharpen an image

        HiddenOpenGLContext.createOpenGL()

        val src = getReference(OS.pictures, "rem-original.jpg")
        val texture = ImageGPUCache.getImage(src, 10000, false)!!

        ShaderLib.init()

        val size = 3

        val ow = texture.w * size
        val oh = texture.h * size

        val upscaled = FBStack["", ow, oh, 4, false, 1, false]
        useFrame(upscaled) { upscale(texture, 0, 0, ow, oh, true, applyToneMapping = false) }
        FramebufferToMemory.createImage(upscaled, false, withAlpha = false)
            .write(src.getSibling("${src.nameWithoutExtension}-${size}x.png"))

        val sharpened = FBStack["", ow, oh, 4, false, 1, false]
        useFrame(sharpened) { sharpen(upscaled.textures.first(), 1f, 0, 0, ow, oh, true) }

        FramebufferToMemory.createImage(sharpened, false, withAlpha = false)
            .write(src.getSibling("${src.nameWithoutExtension}-${size}x-s.png"))

    }


}