package me.anno.ecs.components.shaders.effects

import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFX.flat01
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.ShaderLib.uiVertexShader
import me.anno.gpu.shader.ShaderLib.uiVertexShaderList
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS

object FSR {

    val code = lazy {
        val defines = getReference("res://shaders/fsr1/ffx_a.h").readTextSync()
        val functions = getReference("res://shaders/fsr1/ffx_fsr1.h").readTextSync()
        defines to functions
    }

    private val upscaleShader = lazy {

        val defines = code.value.first
        val functions = code.value.second

        val shader = Shader(
            "upscale", uiVertexShaderList, uiVertexShader, uvList, listOf(
                Variable(GLSLType.V2F, "dstWH"),
                Variable(GLSLType.V3F, "background"),
                Variable(GLSLType.S2D, "source"),
                Variable(GLSLType.V4F, "glFragColor", VariableMode.OUT),
                Variable(GLSLType.V4F, "con0"),
                Variable(GLSLType.V4F, "con1"),
                Variable(GLSLType.V4F, "con2"),
                Variable(GLSLType.V4F, "con3"),
                Variable(GLSLType.V2F, "texelOffset"),
                Variable(GLSLType.V1B, "applyToneMapping"),
                Variable(GLSLType.V1B, "withAlpha")
            ), "" +
                    "#define A_GPU 1\n" +
                    "#define A_GLSL 1\n" +
                    "#define ANNO 1\n" + // we use our custom version
                    defines +
                    "#define FSR_EASU_F 1\n" +
                    (if (OS.isWeb) "#define HLSL\n" else "") +
                    "#ifdef HLSL\n" +
                    "void FsrEasuLoad(vec2 p, out vec4 r, out vec4 g, out vec4 b){\n" +
                    "   vec2 dx = vec2(con1.x,0.0);\n" +
                    "   vec2 dy = vec2(0.0,con1.y);\n" +
                    "   vec2 dxy = con1.xy;\n" +
                    "   vec4 x00 = texture(source,p);\n" +
                    "   vec4 x01 = texture(source,p+dy);\n" +
                    "   vec4 x10 = texture(source,p+dx);\n" +
                    "   vec4 x11 = texture(source,p+dxy);\n" +
                    "   vec3 y00 = withAlpha ? mix(background, x00.rgb, x00.aaa) : x00;\n" +
                    "   vec3 y01 = withAlpha ? mix(background, x01.rgb, x01.aaa) : x01;\n" +
                    "   vec3 y10 = withAlpha ? mix(background, x10.rgb, x10.aaa) : x10;\n" +
                    "   vec3 y11 = withAlpha ? mix(background, x11.rgb, x11.aaa) : x11;\n" +
                    // this is the order of textureGather: https://registry.khronos.org/OpenGL-Refpages/gl4/html/textureGather.xhtml
                    "   r = vec4(y01.r,y11.r,y10.r,y00.r);\n" +
                    "   g = vec4(y01.g,y11.g,y10.g,y00.g);\n" +
                    "   b = vec4(y01.b,y11.b,y10.b,y00.b);\n" +
                    "}\n" +
                    "#else\n" +
                    "void FsrEasuLoad(vec2 p, out vec4 r, out vec4 g, out vec4 b){\n" +
                    "   if(withAlpha){\n" +
                    "       vec4 alpha = textureGather(source,p,3);\n" +
                    "       r = mix(background.rrrr, textureGather(source,p,0), alpha);\n" +
                    "       g = mix(background.gggg, textureGather(source,p,1), alpha);\n" +
                    "       b = mix(background.bbbb, textureGather(source,p,2), alpha);\n" +
                    "   } else {\n" +
                    "       r = textureGather(source,p,0);\n" +
                    "       g = textureGather(source,p,1);\n" +
                    "       b = textureGather(source,p,2);\n" +
                    "   }\n" +
                    "}\n" +
                    "#endif\n" +
                    functions +
                    randomGLSL + // needed for tone mapping
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
            "sharpen", uiVertexShaderList, uiVertexShader, uvList, listOf(
                Variable(GLSLType.V4F, "color", VariableMode.OUT),
                Variable(GLSLType.V2F, "dstWH"),
                Variable(GLSLType.V1F, "sharpness"),
                Variable(GLSLType.S2D, "source"),
            ), "" +
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
                    "   ivec2 coords = ivec2(uv*dstWH);\n" +
                    "   FsrRcasF(color.r,color.g,color.b,color.a,coords,sharpness);\n" +
                    "}"
        )
        shader.glslVersion = 420 // for int->float->int ops, which are used for fast sqrt and such
        shader.use()
        shader

    }

    fun upscale(
        sw: Int, sh: Int, x: Int, y: Int, w: Int, h: Int,
        backgroundColor: Int, flipY: Boolean,
        applyToneMapping: Boolean, withAlpha: Boolean
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
        shader.v1b("withAlpha", withAlpha)
        flat01.draw(shader)
    }

    private fun fsrConfig(shader: Shader, iw: Int, ih: Int, ow: Int, oh: Int) {
        shader.v4f("con0", iw.toFloat() / ow, ih.toFloat() / oh, 0.5f * iw / ow - 0.5f, 0.5f * ih / oh - 0.5f)
        shader.v4f("con1", 1f / iw, 1f / ih, 1f / iw, -1f / ih)
        shader.v4f("con2", -1f / iw, 2f / ih, 1f / iw, 2f / ih)
        shader.v4f("con3", 0f, 4f / ih, 0f, 0f)
    }

    fun upscale(
        source: ITexture2D, x: Int, y: Int, w: Int, h: Int,
        flipY: Boolean, applyToneMapping: Boolean, withAlpha: Boolean
    ) {
        source.bindTrulyNearest(0)
        upscale(source.width, source.height, x, y, w, h, 0, flipY, applyToneMapping, withAlpha)
    }

    fun upscale(
        source: ITexture2D, x: Int, y: Int, w: Int, h: Int, backgroundColor: Int,
        flipY: Boolean, applyToneMapping: Boolean, withAlpha: Boolean
    ) {
        source.bindTrulyNearest(0)
        upscale(source.width, source.height, x, y, w, h, backgroundColor, flipY, applyToneMapping, withAlpha)
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

    fun texelOffset(shader: Shader, w: Int, h: Int) {
        shader.v2f("dstWH", w.toFloat(), h.toFloat())
    }

    fun sharpen(source: ITexture2D, sharpness: Float, x: Int, y: Int, w: Int, h: Int, flipY: Boolean) {
        source.bindTrulyNearest(0)
        sharpen(sharpness, x, y, w, h, flipY)
    }
}