package me.anno.image.svg

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.GFXx3D.shader3DUniforms
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.createShader
import me.anno.gpu.shader.ShaderLib.flatNormal
import me.anno.gpu.shader.ShaderLib.y3D
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths.fract
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import kotlin.math.floor
import kotlin.math.round

/**
 * Draws scalable vector graphics.
 * */
object DrawSVGs {

    val shader3DSVG: BaseShader = run {

        // with texture
        // somehow becomes dark for large |steps|-values

        val vSVGl = listOf(
            Variable(GLSLType.V3F, "aLocalPosition", VariableMode.ATTR),
            Variable(GLSLType.V2F, "aLocalPos2", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aFormula0", VariableMode.ATTR),
            Variable(GLSLType.V1F, "aFormula1", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aColor0", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aColor1", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aColor2", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aColor3", VariableMode.ATTR),
            Variable(GLSLType.V4F, "aStops", VariableMode.ATTR),
            Variable(GLSLType.V1F, "aPadding", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform")
        )

        val vSVG = "" +
                "void main(){\n" +
                "   finalPosition = aLocalPosition;\n" +
                "   gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                flatNormal +
                "   color0 = aColor0;\n" +
                "   color1 = aColor1;\n" +
                "   color2 = aColor2;\n" +
                "   color3 = aColor3;\n" +
                "   stops = aStops;\n" +
                "   padding = aPadding;\n" +
                "   localPos2 = aLocalPos2;\n" +
                "   formula0 = aFormula0;\n" +
                "   formula1 = aFormula1;\n" +
                "}"

        val ySVG = y3D + listOf(
            Variable(GLSLType.V4F, "color0"),
            Variable(GLSLType.V4F, "color1"),
            Variable(GLSLType.V4F, "color2"),
            Variable(GLSLType.V4F, "color3"),
            Variable(GLSLType.V4F, "stops"),
            Variable(GLSLType.V4F, "formula0"), // pos, dir
            Variable(GLSLType.V1F, "formula1"), // is circle
            Variable(GLSLType.V1F, "padding"), // spread method / repetition type
            Variable(GLSLType.V2F, "localPos2"), // position for gradient
        )

        val fSVGl = listOf(
            Variable(GLSLType.V4F, "uvLimits"),
            Variable(GLSLType.S2D, "tex"),
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT)
        )

        val fSVG = "" +
                "bool isInLimits(float value, vec2 minMax){\n" +
                "   return value >= minMax.x && value <= minMax.y;\n" +
                "}\n" + // sqrt and ² for better color mixing
                "vec4 mix2(vec4 a, vec4 b, float stop, vec2 stops){\n" +
                "   float f = clamp((stop-stops.x)/(stops.y-stops.x), 0.0, 1.0);\n" +
                "   return vec4(sqrt(mix(a.rgb*a.rgb, b.rgb*b.rgb, f)), mix(a.a, b.a, f));\n" +
                "}\n" +
                "void main(){\n" +
                // apply the formula; polynomial of 2nd degree
                "   vec2 delta = localPos2 - formula0.xy;\n" +
                "   vec2 dir = formula0.zw;\n" +
                "   float stopValue = formula1 > 0.5 ? length(delta * dir) : dot(dir, delta);\n" +

                "   if(padding < 0.5){\n" + // clamp
                "       stopValue = clamp(stopValue, 0.0, 1.0);\n" +
                "   } else if(padding < 1.5){\n" + // repeat mirrored, and yes, it looks like magic xD
                "       stopValue = 1.0 - abs(fract(stopValue*0.5)*2.0-1.0);\n" +
                "   } else {\n" + // repeat
                "       stopValue = fract(stopValue);\n" +
                "   }\n" +

                // find the correct color
                "   vec4 color = \n" +
                "       stopValue <= stops.x ? color0:\n" +
                "       stopValue >= stops.w ? color3:\n" +
                "       stopValue <  stops.y ? mix2(color0, color1, stopValue, stops.xy):\n" +
                "       stopValue <  stops.z ? mix2(color1, color2, stopValue, stops.yz):\n" +
                "                              mix2(color2, color3, stopValue, stops.zw);\n" +
                "   if(isInLimits(uv.x, uvLimits.xz) && isInLimits(uv.y, uvLimits.yw)){" +
                "       vec4 color2 = color * texture(tex, uv * 0.5 + 0.5);\n" +
                "       finalColor = color2.rgb;\n" +
                "       finalAlpha = color2.a;\n" +
                "   } else {" +
                "       finalColor = vec3(0);\n" +
                "       finalAlpha = 0.0;\n" +
                "   }" +
                "}"

        createShader("3d-svg", vSVGl, vSVG, ySVG, fSVGl, fSVG, listOf("tex"))
    }


    fun draw3DSVG(
        stack: Matrix4fArrayList, buffer: StaticBuffer, texture: ITexture2D, color: Vector4f,
        filtering: Filtering, clamping: Clamping, tiling: Vector4f?
    ) {
        val shader = init(stack, texture, color, filtering, clamping)
        draw(stack, buffer, clamping, tiling, shader)
    }

    fun init(
        stack: Matrix4fArrayList, texture: ITexture2D, color: Vector4f,
        filtering: Filtering, clamping: Clamping
    ): Shader {
        val shader = shader3DSVG.value
        shader.use()
        shader3DUniforms(shader, stack, texture.width, texture.height, color, null)
        texture.bind(0, filtering, clamping)
        return shader
    }

    fun draw(
        stack: Matrix4fArrayList, buffer: StaticBuffer,
        clamping: Clamping, tiling: Vector4f?,
        shader: Shader
    ) {

        // normalized on y-axis, width unknown
        val bounds = buffer.bounds!!
        val sx = bounds.minX / bounds.minY
        val sy = 1f
        if (tiling == null) {

            GFX.check()
            // x2 just for security...
            shader.v4f("uvLimits", -2f * sx, -2f, 2f * sx, 2f)
            GFX.check()
            buffer.draw(shader)
            GFX.check()
        } else {

            // uv[1] = (uv[0]-0.5) * tiling.xy + 0.5 + tiling.zw
            val tx = tiling.x
            val ty = tiling.y
            val tz = tiling.z
            val tw = tiling.w

            val rx = floor(tz).toInt()
            val ry = floor(tw).toInt()
            val fx = fract(tz)
            val fy = fract(tw)
            val x0 = round(-.5f * tx + fx)
            val x1 = round(+.5f * tx + fx)
            val y0 = round(-.5f * ty + fy)
            val y1 = round(+.5f * ty + fy)

            val mirrorRepeat = clamping == Clamping.MIRRORED_REPEAT

            stack.scale(1f / tx, 1f / ty, 1f)

            val count = (x1 - x0) * (y1 - y0)
            if (count > DefaultConfig["objects.svg.tilingCountMax", 10_000]) {
                return
            }

            for (x in x0.toInt()..x1.toInt()) {
                for (y in y0.toInt()..y1.toInt()) {
                    stack.next {

                        stack.translate((x - fx) * sx, (y - fy) * sy, 0f)

                        var mirrorX = tx < 0
                        var mirrorY = ty < 0

                        if (mirrorRepeat) {
                            if ((x + rx).and(1) != 0) mirrorX = !mirrorX
                            if ((y + ry).and(1) != 0) mirrorY = !mirrorY
                        }

                        if (mirrorX || mirrorY) {
                            stack.scale(
                                if (mirrorX) -1f else 1f,
                                if (mirrorY) -1f else 1f,
                                1f
                            )
                        }

                        // calculate left and right borders
                        // works for all tiling values and offsets <3
                        var a0 = -0.5f * tx - x + fx
                        var a1 = +0.5f * tx - x + fx
                        var b0 = -0.5f * ty - y + fy
                        var b1 = +0.5f * ty - y + fy

                        // fix mirrored sharks
                        if (mirrorX) {
                            val t = -a0; a0 = -a1; a1 = t
                        }

                        if (mirrorY) {
                            val t = -b0; b0 = -b1; b1 = t
                        }

                        shader.v4f("uvLimits", sx * a0, b0, sx * a1, b1)
                        buffer.draw(shader)
                        GFX.check()

                    }
                }
            }
        }
    }
}