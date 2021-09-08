package me.anno.ecs.components.shaders.effects

import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.ShaderLib
import me.anno.gpu.TextureLib
import me.anno.gpu.copying.FramebufferToMemory
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.image.ImageGPUCache
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS
import me.anno.utils.io.ResourceHelper

// todo fxaa
// todo by inverse transform between frames
// todo pixel-wise motion vector for that
// todo then mix then, if they are compatible / in-field-of-view
// todo test case: rotate camera by 45°, draw white box on black background:
// todo do we get the correct appearance from just mixing?

// todo integrate this into the engine somehow
object FSR {

    /*fun String.removeComments(): String {
        return split('\n')
            .filter { !it.trim().startsWith("//") }
            .joinToString("\n")
    }*/

    private val vertex = ShaderLib.simpleVertexShader

    val code = lazy {
        val defines = ResourceHelper.loadText("shader/fsr/ffx_a.h")
        val functions = ResourceHelper.loadText("shader/fsr/ffx_fsr1.h")
        defines to functions
    }

    private val upscaleShader = lazy {

        val defines = code.value.first
        val functions = code.value.second

        val shader = Shader(
            "upscale", null, vertex,
            listOf(Variable("vec2", "uv")), "" +
                    "uniform sampler2D source;\n" +
                    "#define A_GPU 1\n" +
                    "#define A_GLSL 1\n" +
                    "#define ANNO 1\n" +
                    defines +
                    "#define FSR_EASU_F 1\n" +
                    "AF4 FsrEasuRF(AF2 p){\n" +
                    "   return textureGather(source,p,0);\n" +
                    "}\n" +
                    "AF4 FsrEasuGF(AF2 p){\n" +
                    "   return textureGather(source,p,1);\n" +
                    "}\n" +
                    "AF4 FsrEasuBF(AF2 p){\n" +
                    "   return textureGather(source,p,2);\n" +
                    "}\n" +
                    functions +
                    "layout(location=0) out vec4 glFragColor;\n" +
                    "uniform vec4 con0,con1,con2,con3;\n" +
                    "uniform vec2 texelOffset;\n" +
                    "void main(){\n" +
                    "   vec3 color;\n" +
                    "   float alpha = texture(source,uv).a;\n" +
                    "   FsrEasuF(color,gl_FragCoord.xy*vec2(1,-1)+texelOffset,con0,con1,con2,con3);\n" +
                    "   glFragColor = vec4(color,alpha);\n" +
                    ");\n" +
                    "}", true
        )
        shader.glslVersion = 420 // for int->float->int ops, which are used for fast sqrt and such
        shader

    }

    private val sharpenShader = lazy {

        val defines = code.value.first
        val functions = code.value.second

        val shader = Shader(
            "upscale", null, vertex,
            listOf(Variable("vec2", "uv")), "" +
                    "out vec4 glFragColor;\n" +
                    "uniform float sharpness;\n" +
                    "uniform vec2 texelOffset;\n" +
                    "uniform sampler2D source;\n" +
                    "#define A_GPU 1\n" +
                    "#define A_GLSL 1\n" +
                    "#define FSR_RCAS_PASSTHROUGH_ALPHA 1\n" +
                    "#define ANNO 1\n" +
                    defines +
                    "#define FSR_RCAS_F 1\n" +
                    "AF4 FsrRcasLoadF(ASU2 p){\n" +
                    "   return texelFetch(source,p,0);\n" +
                    "}\n" +
                    // optional input transform
                    "void FsrRcasInputF(inout AF1 r,inout AF1 g,inout AF1 b){}\n" +
                    functions +
                    "void main(){\n" +
                    "   vec4 color;\n" +
                    "   FsrRcasF(color.r,color.g,color.b,color.a,ivec2(gl_FragCoord.xy*vec2(1,-1)+texelOffset),sharpness);" +
                    "   glFragColor = color;\n" +
                    "}", true
        )
        shader.glslVersion = 420 // for int->float->int ops, which are used for fast sqrt and such
        shader

    }

    fun upscale(sw: Int, sh: Int, x: Int, y: Int, w: Int, h: Int) {
        // if source is null, the texture needs to be bound to slot 0
        val shader = upscaleShader.value
        shader.use()
        fsrConfig(shader, sw, sh, w, h)
        shader.v4("tiling", 1f, 1f, 0f, 0f)
        texelOffset(shader, x, y)
        posSize(shader, x, y, w, h)
        flat01.draw(shader)
    }

    private fun fsrConfig(shader: Shader, iw: Int, ih: Int, ow: Int, oh: Int) {
        shader.v4("con0", iw.toFloat() / ow, ih.toFloat() / oh, 0.5f * iw / ow - 0.5f, 0.5f * ih / oh - 0.5f)
        shader.v4("con1", 1f / iw, 1f / ih, 1f / iw, -1f / ih)
        shader.v4("con2", -1f / iw, 2f / ih, 1f / iw, 2f / ih)
        shader.v4("con3", 0f, 4f / ih, 0f, 0f)
    }

    fun upscale(source: ITexture2D, x: Int, y: Int, w: Int, h: Int) {
        source.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
        upscale(source.w, source.h, x, y, w, h)
    }

    fun sharpen(sharpness: Float, x: Int, y: Int, w: Int, h: Int) {
        val shader = sharpenShader.value
        shader.use()
        shader.v1("sharpness", sharpness)
        texelOffset(shader, x, y)
        posSize(shader, x, y, w, h)
        flat01.draw(shader)
    }

    fun texelOffset(shader: Shader, x: Int, y: Int) {
        shader.v2("texelOffset", -x.toFloat(), (GFX.height - y).toFloat())
    }

    fun sharpen(source: ITexture2D, sharpness: Float, x: Int, y: Int, w: Int, h: Int) {
        source.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
        sharpen(sharpness, x, y, w, h)
    }

    @JvmStatic
    fun main(args: Array<String>) {

        // testing to upscale and sharpen an image

        HiddenOpenGLContext.setSize(1024)
        HiddenOpenGLContext.createOpenGL()

        val src = getReference(OS.pictures, "bg,f8f8f8-flat,750x,075,f-pad,750x1000,f8f8f8.u4.jpg")
        val texture = ImageGPUCache.getImage(src, 10000, false)!!

        ShaderLib.init()
        TextureLib.init()

        val size = 3

        val ow = texture.w * size
        val oh = texture.h * size

        val upscaled = FBStack["", ow, oh, 4, false, 1]
        useFrame(upscaled) { upscale(texture, 0, 0, ow, oh) }
        FramebufferToMemory.createImage(upscaled, false, false)
            .write(src.getSibling("${src.nameWithoutExtension}-${size}x.png"))

        val sharpened = FBStack["", ow, oh, 4, false, 1]
        useFrame(sharpened) { sharpen(upscaled.textures.first(), 1f, 0, 0, ow, oh) }

        FramebufferToMemory.createImage(sharpened, false, false)
            .write(src.getSibling("${src.nameWithoutExtension}-${size}x-s.png"))

    }


}