package me.anno.engine.ui.render

import me.anno.engine.gui.LineShapes
import me.anno.engine.ui.render.GridColors.colorX
import me.anno.engine.ui.render.GridColors.colorY
import me.anno.engine.ui.render.GridColors.colorZ
import me.anno.gpu.OpenGL
import me.anno.gpu.blending.BlendMode
import me.anno.ui.editor.sceneView.Grid
import me.anno.utils.maths.Maths
import kotlin.math.floor
import kotlin.math.log10

object MovingGrid {

    fun drawGrid(radius: Double, worldScale: Double) {

        val powerOf10 = StrictMath.log10(worldScale)
        val roundedPowerOf10 = StrictMath.round(powerOf10)
        val worldScaleX = StrictMath.pow(10.0, roundedPowerOf10.toDouble()) // world scale in smooth decades

        val distance = radius * worldScaleX // if radius = 1/worldScale, then distance is from [0.3162, 3.162]
        // (sqrt(0.1), sqrt(10))

        // done move the grid
        // done fix the scaling issues of the grid:
        // when we are out of 1e30, or 1e-30, we should still draw it
        OpenGL.blendMode.use(BlendMode.ADD) {
            // draw grid
            // scale it based on the radius (movement speed)
            // don't write depth, we want to stack it
            OpenGL.depthMask.use(false) {

                val log = log10(distance)
                val f = (log - floor(log)).toFloat()
                val g = 1f - f
                val cameraDistance = Maths.pow(10.0, floor(log))

                drawGrid(worldScaleX, cameraDistance * 1e1, g)
                drawGrid(worldScaleX, cameraDistance * 1e2, 1f)
                drawGrid(worldScaleX, cameraDistance * 1e3, f)

                drawAxes(worldScale)

            }
        }

    }

    private fun drawGrid(scale: Double, cameraDistance: Double, f: Float) {

        RenderView.stack.pushMatrix()

        RenderView.stack.scale(cameraDistance.toFloat())

        val gridScale = -scale / cameraDistance
        val dy = RenderView.camPosition.y * gridScale
        val dx = Maths.roundFract(RenderView.camPosition.x * gridScale)
        val dz = Maths.roundFract(RenderView.camPosition.z * gridScale)
        RenderView.stack.translate(dx.toFloat(), dy.toFloat(), dz.toFloat())

        val gridAlpha = 0.05f
        Grid.drawGrid(RenderView.stack, gridAlpha * f)

        RenderView.stack.popMatrix()

    }

    private fun drawAxes(scale: Double) {

        val length = 1e5 / scale
        val alpha = 127 shl 24
        LineShapes.drawLine(null, -length, 0.0, 0.0, +length, 0.0, 0.0, colorX or alpha)
        LineShapes.drawLine(null, 0.0, -length, 0.0, 0.0, +length, 0.0, colorY or alpha)
        LineShapes.drawLine(null, 0.0, 0.0, -length, 0.0, 0.0, +length, colorZ or alpha)

    }

}