package me.anno.engine.ui.render

import me.anno.config.DefaultStyle.black
import me.anno.engine.ui.LineShapes
import me.anno.engine.ui.render.GridColors.colorX
import me.anno.engine.ui.render.GridColors.colorY
import me.anno.engine.ui.render.GridColors.colorZ
import me.anno.gpu.OpenGL
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.LineBuffer
import me.anno.maths.Maths
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.round
import kotlin.math.roundToInt

object MovingGrid {

    // todo this isn't fully correct yet, somehow a bit too small often

    fun drawGrid(radius: Double, worldScale: Double) {

        val powerOf10 = StrictMath.log10(worldScale)
        val roundedPowerOf10 = StrictMath.round(powerOf10)
        val worldScaleX = StrictMath.pow(10.0, roundedPowerOf10.toDouble()) // world scale in smooth decades

        val distance = radius * worldScaleX // if radius = 1/worldScale, then distance is from [0.3162, 3.162]

        LineBuffer.finish(RenderView.cameraMatrix)
        OpenGL.blendMode.use(BlendMode.ADD) {
            if (RenderView.currentInstance?.renderMode != RenderMode.DEPTH) {
                // don't write depth, we want to stack it
                OpenGL.depthMask.use(false) {
                    drawGrid2(worldScale, distance)
                }
            } else {
                drawGrid2(worldScale, distance)
            }
        }

    }

    private fun drawGrid2(worldScale: Double, distance: Double) {

        val log = log10(distance)
        val floorLog = floor(log)
        val f = (log - floorLog).toFloat()
        val g = 1f - f
        val cameraDistance = Maths.pow(10.0, floorLog)

        drawGrid(cameraDistance * 1e1, g)
        drawGrid(cameraDistance * 1e2, 1f)
        drawGrid(cameraDistance * 1e3, f)

        drawAxes(worldScale)

        LineBuffer.finish(RenderView.cameraMatrix)

    }

    private fun drawGrid(cameraDistance: Double, f: Float) {

        val position = RenderView.currentInstance?.position ?: RenderView.camPosition

        val dx = round(position.x / cameraDistance) * cameraDistance
        val dz = round(position.z / cameraDistance) * cameraDistance

        val gridAlpha = 0.05f * f
        val alpha = if (RenderView.currentInstance?.renderMode != RenderMode.DEPTH) gridAlpha else 1f
        val color = (alpha * 255).roundToInt() * 0x10101 or black

        // default grid didn't work correctly with depth -> using lines
        for (i in -100..100) {
            val v = 0.01 * cameraDistance * i
            LineShapes.drawLine(null, v + dx, 0.0, cameraDistance + dz, v + dx, 0.0, -cameraDistance + dz, color)
            LineShapes.drawLine(null, cameraDistance + dx, 0.0, v + dz, -cameraDistance + dx, 0.0, v + dz, color)
        }
    }

    private fun drawAxes(scale: Double) {
        val length = 1e5 / scale
        val alpha = 127 shl 24
        LineShapes.drawLine(null, -length, 0.0, 0.0, +length, 0.0, 0.0, colorX or alpha)
        LineShapes.drawLine(null, 0.0, -length, 0.0, 0.0, +length, 0.0, colorY or alpha)
        LineShapes.drawLine(null, 0.0, 0.0, -length, 0.0, 0.0, +length, colorZ or alpha)
    }

}