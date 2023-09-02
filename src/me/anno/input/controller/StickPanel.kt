package me.anno.input.controller

import me.anno.utils.Color.black
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.input.Controller
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.ui.style.Style
import me.anno.utils.types.Floats.f3s
import org.apache.logging.log4j.LogManager

class StickPanel(
    controller: Controller,
    axis0: Int,
    axis1: Int,
    style: Style
) : CalibrationPanel(controller, axis0, axis1, -1, style) {

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(StickPanel::class)
    }

    override fun save(cali: ControllerCalibration) {
        cali.dead[axis0]
        if (length(bounds.deltaX, bounds.deltaY) > 0.5f) {
            cali.scale[axis0] = 2f / max(1e-7f, bounds.deltaX - dead.deltaX)
            cali.scale[axis1] = 2f / max(1e-7f, bounds.deltaY - dead.deltaY)
            if (stillNanos > 0L) {
                cali.dead[axis0] = dead.deltaX * 0.75f // 0.5f because half; 1.5x for safety
                cali.dead[axis1] = dead.deltaY * 0.75f
                cali.center[axis0] = dead.centerX
                cali.center[axis1] = dead.centerY
            } else {
                // default
                LOGGER.warn("Missing dead spots / center for $axis0/$axis1")
                cali.dead[axis0] = 0.1f
                cali.dead[axis1] = 0.1f
                cali.center[axis0] = 0f
                cali.center[axis1] = 0f
            }
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)

        val vx = controller.getRawAxis(axis0)
        val vy = controller.getRawAxis(axis1)

        drawSimpleTextCharByChar(x, y, 2, "${vx.f3s()}, ${vy.f3s()}")

        cross(vx, vy, 0.3f, -1)

        showBounds(bounds, -1)
        showBounds(dead, 0xff7777 or black)

    }

}
