package me.anno.ecs.components.shaders.effects

import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFX
import me.anno.gpu.GFX.flat01
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.texture.ITexture2D
import me.anno.io.ResourceHelper
import me.anno.utils.OS

object FSR {

    private val vertex = ShaderLib.simpleVertexShader

    val code = lazy {
        val defines = ResourceHelper.loadText("shader/fsr1/ffx_a.h")
        val functions = ResourceHelper.loadText("shader/fsr1/ffx_fsr1.h")
        defines to functions
    }

    private val upscaleShader = lazy {

        val defines = code.value.first
        val functions = code.value.second

        val shader = Shader(
            "upscale", coordsList, vertex, uvList, emptyList(), "" +
                    "uniform vec2 dstWH;\n" +
                    "uniform vec3 background;\n" +
                    "uniform sampler2D source;\n" +
                    "#define A_GPU 1\n" +
                    "#define A_GLSL 1\n" +
                    "#define ANNO 1\n" + // we use our custom version
                    defines +
                    "layout(location=0) out vec4 glFragColor;\n" +
                    "uniform vec4 con0,con1,con2,con3;\n" +
                    "uniform vec2 texelOffset;\n" +
                    "uniform bool applyToneMapping;\n" +
                    "#define FSR_EASU_F 1\n" +
                    if (OS.isWeb) {
                        "void FsrEasuLoad(vec2 p, out vec4 r, out vec4 g, out vec4 b){\n" +
                                "   vec2 dx = vec2(con1.x,0.0);\n" +
                                "   vec2 dy = vec2(0.0,con1.y);\n" +
                                "   vec2 dxy = con1.xy;\n" +
                                "   vec4 x00 = texture(source,p);\n" +
                                "   vec3 y00 = mix(background, x00.rgb, x00.aaa);\n" +
                                "   vec4 x01 = texture(source,p+dy);\n" +
                                "   vec3 y01 = mix(background, x01.rgb, x01.aaa);\n" +
                                "   vec4 x10 = texture(source,p+dx);\n" +
                                "   vec3 y10 = mix(background, x10.rgb, x10.aaa);\n" +
                                "   vec4 x11 = texture(source,p+dxy);\n" +
                                "   vec3 y11 = mix(background, x11.rgb, x11.aaa);\n" +
                                // this is the order of textureGather: https://registry.khronos.org/OpenGL-Refpages/gl4/html/textureGather.xhtml
                                "   r = vec4(y01.r,y11.r,y10.r,y00.r);\n" +
                                "   g = vec4(y01.g,y11.g,y10.g,y00.g);\n" +
                                "   b = vec4(y01.b,y11.b,y10.b,y00.b);\n" +
                                "}\n"
                    } else {
                        "void FsrEasuLoad(vec2 p, out vec4 r, out vec4 g, out vec4 b){\n" +
                                "   vec4 alpha = textureGather(source,p,3);\n" +
                                "   r = mix(background.rrrr, textureGather(source,p,0), alpha);\n" +
                                "   g = mix(background.gggg, textureGather(source,p,1), alpha);\n" +
                                "   b = mix(background.bbbb, textureGather(source,p,2), alpha);\n" +
                                "}\n"
                    } +
                    functions +
                    noiseFunc + // needed for tone mapping
                    tonemapGLSL +
                    "void main(){\n" +
                    "   vec3 color;\n" +
                    "   float alpha = texture(source,uv).a;\n" +
                    "   vec2 coords = uv * dstWH;\n" +
                    "   FsrEasuF(color, coords, con0, con1, con2, con3);\n" +
                    "   glFragColor = vec4(applyToneMapping ? tonemap(color) : color, alpha);\n" +
                    "}"
        )
        shader.glslVersion = 420 // for int->float->int ops, which are used for fast sqrt and such
        shader
    }

    private val sharpenShader = lazy {

        val defines = code.value.first
        val functions = code.value.second

        val shader = Shader(
            "upscale", coordsList, vertex, uvList, emptyList(), "" +
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
                    "void FsrRcasInputF(inout float r,inout float g,inout float b){}\n" +
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
        shader.v3f("background", 0)
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
        shader.v3f("background", backgroundColor)
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
        source.bindTrulyNearest(0)
        upscale(source.w, source.h, x, y, w, h, flipY, applyToneMapping)
    }

    fun upscale(
        source: ITexture2D, x: Int, y: Int, w: Int, h: Int,
        flipY: Boolean, backgroundColor: Int, applyToneMapping: Boolean
    ) {
        source.bindTrulyNearest(0)
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
        source.bindTrulyNearest(0)
        sharpen(sharpness, x, y, w, h, flipY)
    }

    fun sharpen(source: ITexture2D, sharpness: Float, flipY: Boolean) {
        source.bindTrulyNearest(0)
        sharpen(sharpness, flipY)
    }

}