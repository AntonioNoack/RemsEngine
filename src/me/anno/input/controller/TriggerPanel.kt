package me.anno.input.controller

import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Controller
import me.anno.ui.style.Style
import org.apache.logging.log4j.LogManager

class TriggerPanel(
    controller: Controller,
    axis: Int,
    style: Style
) : CalibrationPanel(controller, axis, -1, -1, style) {

    companion object {
        private val LOGGER = LogManager.getLogger(TriggerPanel::class)
    }

    override val canDrawOverBorders = true

    init {
        tooltip = "Click to reset"
        addLeftClickListener { reset() }
    }

    override fun save(cali: ControllerCalibration) {
        if (!bounds.isEmpty() && bounds.deltaX() > dead.deltaX()) {
            cali.dead[axis0] = dead.deltaX() * 0.75f
            cali.center[axis0] = dead.avgX()
            cali.scale[axis0] = bounds.deltaX() - dead.deltaX()
        } else {
            // default
            LOGGER.warn("Missing calibration for axis $axis0")
            cali.dead[axis0] = 0.1f
            cali.center[axis0] = -1f
            cali.scale[axis0] = 0.5f
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        // -1 = start, +1 = end
        val v01 = controller.getRawAxis(axis0) * .5f + .5f
        val v0h = (v01 * h).toInt()
        drawRect(x0, y + v0h, x1 - x0, 1, -1)
        // todo show bounds and dead spot
    }

}
