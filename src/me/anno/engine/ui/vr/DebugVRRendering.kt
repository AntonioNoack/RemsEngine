package me.anno.engine.ui.vr

import me.anno.config.DefaultConfig
import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFX
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.drawing.GFXx2D.noTiling
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.applyTiling
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import org.joml.Vector4f

object DebugVRRendering {

    private const val applyTonemappingToCol = "" +
            "   col = max(col,vec3(0.0));\n" +
            "   col = pow(col,vec3(gamma));\n" +
            "   if(!(col.x >= -1e38 && col.x <= 1e38)) { col = vec3(1.0,0.0,1.0); }\n" +
            "   else { col = tonemap(col); }\n" +
            "   result = vec4(col,1.0);\n"

    /**
     * https://en.wikipedia.org/wiki/Anaglyph_3D
     * */
    private val redCyanShader = Shader(
        "redCyanShader",
        ShaderLib.uiVertexShaderList,
        ShaderLib.uiVertexShader, uvList,
        listOf(
            Variable(GLSLType.V1B, "applyToneMapping"),
            Variable(GLSLType.V1F, "gamma"),
            Variable(GLSLType.V4F, "leftTiling"),
            Variable(GLSLType.V4F, "rightTiling"),
            Variable(GLSLType.S2D, "leftTexture"),
            Variable(GLSLType.S2D, "rightTexture"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                tonemapGLSL +
                applyTiling +
                "void main(){\n" +
                "   vec3 col = mix(\n" +
                "       texture(leftTexture, applyTiling(uv, leftTiling)).rgb,\n" +
                "       texture(rightTexture, applyTiling(uv, rightTiling)).rgb,\n" +
                "       vec3(0.0, 1.0, 1.0));\n" +
                applyTonemappingToCol +
                "}"
    )

    private val simpleShader = Shader(
        "redCyanShader",
        ShaderLib.uiVertexShaderList,
        ShaderLib.uiVertexShader, uvList,
        listOf(
            Variable(GLSLType.V1B, "applyToneMapping"),
            Variable(GLSLType.V1F, "gamma"),
            Variable(GLSLType.V4F, "leftTiling"),
            Variable(GLSLType.S2D, "leftTexture"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                tonemapGLSL +
                applyTiling +
                "void main() {\n" +
                "   vec3 col = texture(leftTexture, applyTiling(uv, leftTiling)).rgb;\n" +
                applyTonemappingToCol +
                "}"
    )

    fun showStereoView(
        x: Int, y: Int, w: Int, h: Int,
        leftTexture: ITexture2D, leftView: Vector4f?,
        rightTexture: ITexture2D, rightView: Vector4f?,
        gamma: Float, showBoth: Boolean
    ) {
        GFX.check()
        val shader = if (showBoth) redCyanShader else simpleShader
        shader.use()
        posSize(shader, x, y, w, h, true)
        noTiling(shader)
        shader.v1f("gamma", gamma)
        shader.v4f("leftTiling", leftView ?: noTiling)
        shader.v4f("rightTiling", rightView ?: noTiling)
        leftTexture.bindTrulyLinear(shader, "leftTexture")
        rightTexture.bindTrulyLinear(shader, "rightTexture")
        flat01.draw(shader)
        GFX.check()
    }
}