package me.anno.gpu

import me.anno.config.DefaultConfig
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.drawing.GFXx2D.defineAdvancedGraphicalFeatures
import me.anno.gpu.drawing.GFXx3D.shader3DUniforms
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.objects.GFXTransform
import me.anno.utils.maths.Maths.fract
import org.joml.Matrix4fArrayList
import org.joml.Vector4fc
import kotlin.math.floor
import kotlin.math.round

object SVGxGFX {

    fun draw3DSVG(
        video: GFXTransform?, time: Double,
        stack: Matrix4fArrayList, buffer: StaticBuffer, texture: Texture2D, color: Vector4fc,
        filtering: Filtering, clamping: Clamping, tiling: Vector4fc?
    ) {

        // normalized on y-axis, width unknown
        val sx = (buffer.minX / buffer.minY).toFloat()
        val sy = 1f
        if (tiling == null) {

            GFX.check()
            val shader = ShaderLib.shader3DSVG.value
            shader.use()
            shader3DUniforms(shader, stack, texture.w, texture.h, color, null, filtering, null)
            defineAdvancedGraphicalFeatures(shader, video, time)
            // x2 just for security...
            shader.v4("uvLimits", -2f * sx, -2f, 2f * sx, 2f)
            GFX.check()
            texture.bind(0, filtering, clamping)
            GFX.check()
            buffer.draw(shader)
            GFX.check()

        } else {

            // uv[1] = (uv[0]-0.5) * tiling.xy + 0.5 + tiling.zw
            val tx = tiling.x()
            val ty = tiling.y()
            val tz = tiling.z()
            val tw = tiling.w()

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

                        val shader = ShaderLib.shader3DSVG.value
                        shader.use()
                        shader3DUniforms(shader, stack, texture.w, texture.h, color, null, filtering, null)
                        defineAdvancedGraphicalFeatures(shader, video, time)
                        shader.v4("uvLimits", sx * a0, b0, sx * a1, b1)
                        texture.bind(0, filtering, clamping)
                        buffer.draw(shader)
                        GFX.check()

                    }
                }
            }

        }
    }

}