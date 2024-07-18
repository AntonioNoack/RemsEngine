package me.anno.openxr

import me.anno.input.controller.Controller
import me.anno.input.controller.ControllerType

// axes: 2 for triggers + 2 for thumbstick
// buttons: 2 for a/b, 2 for triggers, 4 for thumbstick corners, 1 for thumbstick click, 1 for menu button
class OpenXRController : Controller(10, 4) {

    override val numButtons: Int
        get() = 10
    override val numAxes: Int
        get() = 4

    override val type: ControllerType
        get() = ControllerType.VIRTUAL_REALITY

    companion object {
        val xrControllers = listOf(
            OpenXRController(),
            OpenXRController()
        )

        init {
            // can we get their names programmatically? (for calibration)
            xrControllers[0].name = "LeftVRController"
            xrControllers[1].name = "RightVRController"
        }
    }
}