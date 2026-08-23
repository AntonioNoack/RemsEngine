package me.anno.input.controller

import me.anno.ui.canvas.Canvas
import me.anno.ui.Style
import org.apache.logging.log4j.LogManager

class TriggerPanel(
    controller: Controller,
    axis: Int,
    style: Style
) : CalibrationPanel(controller, axis, -1, -1, style) {

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(TriggerPanel::class)
    }

    override val canDrawOverBorders get() = true

    init {
        // click is unusual for controllers, so I think it's a fine control scheme
        tooltip = "Click to reset"
        addLeftClickListener { reset() }
    }

    override fun save(cali: ControllerCalibration) {
        if (!bounds.isEmpty() && bounds.deltaX > dead.deltaX) {
            cali.deadZone[axis0] = dead.deltaX * 0.75f
            cali.center[axis0] = dead.centerX
            cali.scale[axis0] = bounds.deltaX - dead.deltaX
        } else {
            // default
            LOGGER.warn("Missing calibration for axis $axis0")
            cali.deadZone[axis0] = 0.1f
            cali.center[axis0] = -1f
            cali.scale[axis0] = 0.5f
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        // -1 = start, +1 = end
        val v01 = controller.getRawAxis(axis0) * .5f + .5f
        val v0h = (v01 * height).toInt()
        canvas.drawRect(canvas.x0, y + v0h, canvas.x1 - canvas.x0, 1, -1)
        // todo show bounds and dead spot
    }

}
