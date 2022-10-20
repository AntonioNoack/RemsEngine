package me.anno.graph.render

import me.anno.engine.ui.render.Renderers
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector4f

class Texture {

    constructor(v: ITexture2D) {
        this.v2d = v
    }

    constructor(v: Vector4f) {
        tint.set(v)
    }

    constructor(v: Int) {
        v.toVecRGBA(tint)
    }

    constructor(formula: String, formulaParams: List<Any>) {
        this.formula = formula
        this.formulaParams = formulaParams
    }

    var v2d: ITexture2D? = null

    // var v3d: Texture3D? = null
    val tint = Vector4f(1f)

    var formula: String? = null
    var formulaParams: List<Any?>? = null

    class SimpleTexture(
        val tex: ITexture2D,
        val mapR: Vector4f,
        val mapG: Vector4f,
        val mapB: Vector4f,
        val mapA: Vector4f
    ) {
        constructor(tex: ITexture2D, tint: Vector4f) :
                this(
                    tex,
                    Vector4f(tint.x, 0f, 0f, 0f),
                    Vector4f(0f, tint.y, 0f, 0f),
                    Vector4f(0f, 0f, tint.z, 0f),
                    Vector4f(0f, 0f, 0f, tint.w)
                )
    }

    fun toTexture(): SimpleTexture {
        return when (val formula = formula) {
            null -> {
                val v2d = v2d
                if (v2d != null) {
                    SimpleTexture(v2d, tint)
                } else {
                    SimpleTexture(whiteTexture, tint)
                }
            }
            "map" -> {
                val mapping = formulaParams!![0] as String
                val r = mapping[0]
                val g = mapping.getOrNull(1) ?: 'g'
                val b = mapping.getOrNull(1) ?: 'b'
                val a = mapping.getOrNull(1) ?: 'a'
                SimpleTexture(
                    v2d ?: whiteTexture,
                    mappings[r]!!,
                    mappings[g]!!,
                    mappings[b]!!,
                    mappings[a]!!
                )
            }
            else -> TODO(formula)
        }
    }

    fun draw(x: Int, y: Int, w: Int, h: Int) {
        when (val formula = formula) {
            null -> {
                val v2d = v2d
                if (v2d != null) {
                    drawTexture(x, y, w, h, v2d, false, tint)
                } else {
                    drawRect(x, y, w, h, tint)
                }
            }
            "map" -> {
                val mapping = formulaParams!![0] as String
                val r = mapping[0]
                val g = mapping.getOrNull(1) ?: ' '
                val b = mapping.getOrNull(2) ?: ' '
                val a = mapping.getOrNull(3) ?: ' '
                drawMappedTexture(
                    x, y, w, h,
                    v2d ?: whiteTexture,
                    mappings[r]!!,
                    mappings[g]!!,
                    mappings[b]!!,
                    mappings[a]!!,
                    ignoreAlpha = false,
                    applyToneMapping = false
                )
            }
            else -> TODO(formula)
        }
    }

    companion object {

        val mappingShader = BaseShader(
            "mapShaderTexture",
            ShaderLib.simpleVertexShaderV2List,
            ShaderLib.simpleVertexShaderV2, ShaderLib.uvList,
            listOf(
                Variable(GLSLType.V1I, "alphaMode"), // 0 = rgba, 1 = rgb, 2 = a
                Variable(GLSLType.V1B, "applyToneMapping"),
                Variable(GLSLType.V4F, "mapR"),
                Variable(GLSLType.V4F, "mapG"),
                Variable(GLSLType.V4F, "mapB"),
                Variable(GLSLType.V4F, "mapA"),
                Variable(GLSLType.S2D, "tex"),
            ), "" +
                    Renderers.tonemapGLSL +
                    "float map(vec4 map, vec4 col, float def){\n" +
                    "   if(all(lessThan(map, vec4(0.1)))) return def;\n" +
                    "   return dot(map,col);\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   vec4 col = vec4(1.0);\n" +
                    "   if(alphaMode == 0) col *= texture(tex, uv);\n" +
                    "   else if(alphaMode == 1) col.rgb *= texture(tex, uv).rgb;\n" +
                    "   else col.rgb *= texture(tex, uv).a;\n" +
                    "   col = vec4(\n" +
                    "       map(mapR,col,0.0),\n" +
                    "       map(mapG,col,0.0),\n" +
                    "       map(mapB,col,0.0),\n" +
                    "       1.0//map(mapA,col,1.0)\n" +
                    "   );\n" +
                    "   if(applyToneMapping) col = tonemap(col);\n" +
                    "   gl_FragColor = col;\n" +
                    "}"
        )

        fun drawMappedTexture(
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            tex: SimpleTexture,
            ignoreAlpha: Boolean,
            applyToneMapping: Boolean
        ) {
            drawMappedTexture(
                x, y, w, h,
                tex.tex,
                tex.mapR,
                tex.mapG,
                tex.mapB,
                tex.mapA,
                ignoreAlpha,
                applyToneMapping
            )
        }

        fun drawMappedTexture(
            x: Int, y: Int, w: Int, h: Int,
            tex: ITexture2D,
            mapR: Vector4f,
            mapG: Vector4f,
            mapB: Vector4f,
            mapA: Vector4f,
            ignoreAlpha: Boolean,
            applyToneMapping: Boolean
        ) {
            if (w == 0 || h == 0) return
            GFX.check()
            val shader = mappingShader.value
            shader.use()
            GFXx2D.posSize(shader, x, y, w, h)
            GFXx2D.defineAdvancedGraphicalFeatures(shader)
            shader.v1i("alphaMode", ignoreAlpha.toInt())
            shader.v1b("applyToneMapping", applyToneMapping)
            shader.v4f("mapR", mapR)
            shader.v4f("mapG", mapG)
            shader.v4f("mapB", mapB)
            shader.v4f("mapA", mapA)
            GFXx2D.tiling(shader, null)
            val tex1 = tex as? Texture2D
            tex.bind(
                0,
                tex1?.filtering ?: GPUFiltering.NEAREST,
                tex1?.clamping ?: Clamping.CLAMP
            )
            GFX.flat01.draw(shader)
            GFX.check()
        }

        val mappings = mapOf(
            'r' to Vector4f(1f, 0f, 0f, 0f),
            'g' to Vector4f(0f, 1f, 0f, 0f),
            'b' to Vector4f(0f, 0f, 1f, 0f),
            'a' to Vector4f(0f, 0f, 0f, 1f),
            ' ' to Vector4f()
        )
    }

}