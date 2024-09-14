package me.anno.gpu.shader.effects

import me.anno.engine.ui.render.Renderers
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.utils.OS
import me.anno.utils.OS.res
import me.anno.utils.async.Callback.Companion.mapCallback
import me.anno.utils.async.LazyPromise
import kotlin.math.min

object FSR {

    private val code = LazyPromise { cb ->
        listOf("shaders/fsr1/ffx_a.h", "shaders/fsr1/ffx_fsr1.h")
            .mapCallback({ _, fileName, cbI ->
                res.getChild(fileName).readText(cbI)
            }, cb)
    }

    private val upscaleShader = code.then { (defines, functions) ->
        val shader = Shader(
            "upscale", ShaderLib.uiVertexShaderList, ShaderLib.uiVertexShader, ShaderLib.uvList, listOf(
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
                Variable(GLSLType.V1I, "numChannels")
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
                    "   vec3 y00, y01, y10, y11;\n" +
                    "   if(numChannels == 4) {\n" +
                    "       y00 = mix(background, x00.rgb, x00.aaa);\n" +
                    "       y01 = mix(background, x01.rgb, x01.aaa);\n" +
                    "       y10 = mix(background, x10.rgb, x10.aaa);\n" +
                    "       y11 = mix(background, x11.rgb, x11.aaa);\n" +
                    "   } else if(numChannels == 3) {\n" +
                    "       y00 = x00.rgb;\n" +
                    "       y01 = x01.rgb;\n" +
                    "       y10 = x10.rgb;\n" +
                    "       y11 = x11.rgb;\n" +
                    "   } else {\n" +
                    "       y00 = x00.rrr;\n" +
                    "       y01 = x01.rrr;\n" +
                    "       y10 = x10.rrr;\n" +
                    "       y11 = x11.rrr;\n" +
                    "   }\n" +
                    // this is the order of textureGather: https://registry.khronos.org/OpenGL-Refpages/gl4/html/textureGather.xhtml
                    "   r = vec4(y01.r,y11.r,y10.r,y00.r);\n" +
                    "   g = vec4(y01.g,y11.g,y10.g,y00.g);\n" +
                    "   b = vec4(y01.b,y11.b,y10.b,y00.b);\n" +
                    "}\n" +
                    "#else\n" +
                    "void FsrEasuLoad(vec2 p, out vec4 r, out vec4 g, out vec4 b){\n" +
                    "   if(numChannels == 4) {\n" +
                    "       vec4 alpha = textureGather(source,p,3);\n" +
                    "       r = mix(background.rrrr, textureGather(source,p,0), alpha);\n" +
                    "       g = mix(background.gggg, textureGather(source,p,1), alpha);\n" +
                    "       b = mix(background.bbbb, textureGather(source,p,2), alpha);\n" +
                    "   } else if(numChannels == 3) {\n" +
                    "       r = textureGather(source,p,0);\n" +
                    "       g = textureGather(source,p,1);\n" +
                    "       b = textureGather(source,p,2);\n" +
                    "   } else {\n" +
                    "       r = g = b = textureGather(source,p,0);\n" +
                    "   }\n" +
                    "}\n" +
                    "#endif\n" +
                    functions +
                    ShaderFuncLib.randomGLSL + // needed for tone mapping
                    Renderers.tonemapGLSL +
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

    private val sharpenShader = code.then { (defines, functions) ->
        val shader = Shader(
            "sharpen", ShaderLib.uiVertexShaderList, ShaderLib.uiVertexShader, ShaderLib.uvList, listOf(
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
        source: ITexture2D, x: Int, y: Int, w: Int, h: Int,
        backgroundColor: Int, flipY: Boolean,
        applyToneMapping: Boolean, withAlpha: Boolean
    ) {
        // if source is null, the texture needs to be bound to slot 0
        val shader = upscaleShader.value
        if (shader != null) {
            shader.use()
            source.bindTrulyLinear(0)
            fsrConfig(shader, source.width, source.height, w, h)
            tiling(shader, flipY)
            texelOffset(shader, w, h)
            GFXx2D.posSize(shader, x, y, w, h)
            shader.v3f("background", backgroundColor)
            shader.v1b("applyToneMapping", applyToneMapping)
            shader.v1i("numChannels", min(if (withAlpha) 4 else 3, source.channels))
            flat01.draw(shader)
        } else {
            source.bindTrulyLinear(0)
            DrawTextures.drawTexture(x, if (flipY) y + h else y, w, if (flipY) -h else h, source)
        }
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
        upscale(source, x, y, w, h, 0, flipY, applyToneMapping, withAlpha)
    }

    private fun tiling(shader: Shader, flipY: Boolean) {
        shader.v4f("tiling", 1f, if (flipY) -1f else +1f, 0f, 0f)
    }

    private fun texelOffset(shader: Shader, w: Int, h: Int) {
        shader.v2f("dstWH", w.toFloat(), h.toFloat())
    }

    fun sharpen(source: ITexture2D, sharpness: Float, x: Int, y: Int, w: Int, h: Int, flipY: Boolean) {
        val shader = sharpenShader.value
        if (shader != null) {
            shader.use()
            shader.v1f("sharpness", sharpness)
            source.bindTrulyLinear(0)
            texelOffset(shader, w, h)
            tiling(shader, flipY)
            GFXx2D.posSize(shader, x, y, w, h)
            flat01.draw(shader)
        } else {
            source.bindTrulyLinear(0)
            DrawTextures.drawTexture(x, if (flipY) y + h else y, w, if (flipY) -h else h, source)
        }
    }
}