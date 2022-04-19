package me.anno.engine.ui.render

import me.anno.config.DefaultConfig.defaultFont
import me.anno.config.DefaultStyle.black
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.GridColors.colorX
import me.anno.engine.ui.render.GridColors.colorY
import me.anno.engine.ui.render.GridColors.colorZ
import me.anno.fonts.FontManager
import me.anno.fonts.mesh.TextMeshGroup
import me.anno.gpu.OpenGL
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.gpu.drawing.GFXx3D
import me.anno.maths.Maths
import me.anno.maths.Maths.PIf
import me.anno.utils.Clock
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.JomlPools
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.round
import kotlin.math.roundToInt

object MovingGrid {

    fun drawGrid(radius: Double) {

        LineBuffer.finish(RenderView.cameraMatrix)
        OpenGL.blendMode.use(BlendMode.ADD) {
            if (RenderView.currentInstance?.renderMode != RenderMode.DEPTH) {
                // don't write depth, we want to stack it
                OpenGL.depthMask.use(false) {
                    drawGrid2(radius)
                }
            } else {
                drawGrid2(radius)
            }
        }

    }

    private fun drawGrid2(radius: Double) {

        val log = log10(radius)
        val floorLog = floor(log)
        val f = (log - floorLog).toFloat()
        val g = 1f - f
        val radius2 = Maths.pow(10.0, floorLog)

        drawGrid(radius2 * 1e1, g)
        drawGrid(radius2 * 1e2, 1f)
        drawGrid(radius2 * 1e3, f)

        drawAxes(radius)

        LineBuffer.finish(RenderView.cameraMatrix)

    }

    private fun drawGrid(radius: Double, f: Float) {

        val position = RenderView.currentInstance?.position ?: RenderView.camPosition

        val dx = round(position.x / radius) * radius
        val dz = round(position.z / radius) * radius

        val gridAlpha = 0.05f * f
        val alpha = if (RenderView.currentInstance?.renderMode != RenderMode.DEPTH) gridAlpha else 1f
        val color = (alpha * 255).roundToInt() * 0x10101 or black

        // default grid didn't work correctly with depth -> using lines
        for (i in -100..100) {
            val v = 0.01 * radius * i
            LineShapes.drawLine(null, v + dx, 0.0, radius + dz, v + dx, 0.0, -radius + dz, color)
            LineShapes.drawLine(null, radius + dx, 0.0, v + dz, -radius + dx, 0.0, v + dz, color)
        }

        val textAlpha = 0.1f * f
        drawText(radius * 0.01, 1, textAlpha)
        drawText(radius * 0.01, 5, textAlpha)

    }

    // val clock = Clock()

    fun drawText(baseSize: Double, factor: Int, alpha: Float) {
        val size = baseSize * factor
        // clock.start()
        val tmg = texts.getOrPut(size) {
            // format size
            val suffix = when (val power = round(log10(baseSize)).toInt()) {
                -12 -> "pm"
                -11 -> "0pm"
                -10 -> "00pm"
                -9 -> "nm"
                -8 -> "0nm"
                -7 -> "00nm"
                -6 -> "µm"
                -5 -> "0µm"
                -4 -> "00µm"
                -3 -> "mm"
                -2 -> "cm"
                -1 -> "0cm"
                0 -> "m"
                1 -> "0m"
                2 -> "00m"
                3 -> "km"
                4 -> "0km"
                5 -> "00km"
                6 -> "Mm"
                7 -> "0Mm"
                8 -> "00Mm"
                100 -> "Googol m"
                else -> "e${power}m"
            }
            val text = "$factor$suffix"
            val font = FontManager.getFont(defaultFont).font
            TextMeshGroup(font, text, 0f, false, debugPieces = false)
        }
        if (tmg.buffer == null) tmg.createStaticBuffer()
        // clock.stop { "Generating text ${tmg.text}" }, 1ms on average
        val buffer = tmg.buffer ?: return
        val offset = JomlPools.vec3f.create().set(0f)
        // position correctly
        val stack = RenderView.stack
        stack.set(RenderView.cameraMatrix)
        val pos = RenderView.camPosition
        val ws = RenderView.worldScale
        stack.translate(-(pos.x * ws).toFloat(), -(pos.y * ws).toFloat(), -(pos.z * ws).toFloat())
        stack.rotateX(-PIf * 0.5f)
        val sizeF = (size * ws).toFloat()
        stack.translate(sizeF, 0f, 0f)
        stack.scale(sizeF, sizeF, sizeF)
        GFXx3D.draw3DText(offset, stack, buffer, (-1).withAlpha(alpha))
    }

    val texts = HashMap<Double, TextMeshGroup>()

    private fun drawAxes(scale: Double) {
        val length = 1e3 * scale
        val alpha = 127 shl 24
        LineShapes.drawLine(null, -length, 0.0, 0.0, +length, 0.0, 0.0, colorX or alpha)
        LineShapes.drawLine(null, 0.0, -length, 0.0, 0.0, +length, 0.0, colorY or alpha)
        LineShapes.drawLine(null, 0.0, 0.0, -length, 0.0, 0.0, +length, colorZ or alpha)
    }

}