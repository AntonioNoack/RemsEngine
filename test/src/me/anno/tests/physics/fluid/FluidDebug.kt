package me.anno.tests.physics.fluid

import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D

object FluidDebug {

    private val textureRShader = BaseShader(
        "flatShaderTexture",
        ShaderLib.uiVertexShaderList,
        ShaderLib.uiVertexShader, ShaderLib.uvList,
        listOf(Variable(GLSLType.S2D, "tex")), "" +
                "void main(){\n" +
                "   float col = texture(tex, uv).x;\n" +
                "   if(!(col >= -1e38 && col <= 1e38)) {\n" +
                "       gl_FragColor = vec4(1.0,0.0,1.0,1.0);\n" +
                "   } else {\n" +
                "       gl_FragColor = vec4(abs(col) * (col < 0.0 ? vec3(1,0,0) : vec3(1,1,1)), 1.0);\n" +
                "   }\n" +
                "}"
    )

    private val textureRGShader = BaseShader(
        "flatShaderTexture",
        ShaderLib.uiVertexShaderList,
        ShaderLib.uiVertexShader, ShaderLib.uvList,
        listOf(Variable(GLSLType.S2D, "tex")), "" +
                "void main(){\n" +
                "   vec2 col = texture(tex, uv).xy;\n" +
                "   if(!(col.x >= -1e38 && col.x <= 1e38)) {\n" +
                "       gl_FragColor = vec4(1.0,0.0,1.0,1.0);\n" +
                "   } else {\n" +
                "       gl_FragColor = vec4(col.x*.5+.5,col.y*.5+.5,.5, 1.0);\n" +
                "   }\n" +
                "}"
    )

    fun displayTextureR(x: Int, y: Int, w: Int, h: Int, texture: ITexture2D) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = textureRShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        GFXx2D.tiling(shader, null)
        texture.bind(
            0,
            Filtering.NEAREST,
            Clamping.CLAMP
        )
        GFX.flat01.draw(shader)
        GFX.check()
    }

    fun displayTextureRG(x: Int, y: Int, w: Int, h: Int, texture: ITexture2D) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = textureRGShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        GFXx2D.tiling(shader, null)
        texture.bind(
            0,
            Filtering.NEAREST,
            Clamping.CLAMP
        )
        GFX.flat01.draw(shader)
        GFX.check()
    }
}