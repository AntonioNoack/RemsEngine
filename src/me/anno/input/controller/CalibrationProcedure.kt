package me.anno.input.controller

import me.anno.gpu.OSWindow
import me.anno.input.Controller
import me.anno.input.Controller.Companion.saveCalibration
import me.anno.input.Input.controllers
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.SpyPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.Style

object CalibrationProcedure {

    @JvmStatic
    fun start(window: OSWindow, style: Style) {
        val oldValue = Controller.enableControllerInputs
        Controller.enableControllerInputs = false // disable temporarily
        val ws = window.windowStack
        val listY = PanelListY(style)
        listY.add(TextPanel("1. [Range] Move your sticks around in circles", style))
        listY.add(
            TextPanel(
                "2. [Dead Zone, Center] For 8-16 directions, press the stick, " + "and let it back to zero. After that wait two seconds each.",
                style
            )
        )
        listY.add(TextPanel("3. Press 'Save'", style))
        val listX = PanelListX(style)
        listY.add(listX)
        for (i in controllers.indices) {
            listX.add(createWindow(controllers[i], style))
        }
        val window1 = object : Window(listY, true, ws) {
            override fun destroy() {
                super.destroy()
                Controller.enableControllerInputs = oldValue
            }
        }
        ws.push(window1)
    }

    @JvmStatic
    private fun createWindow(controller: Controller, style: Style): Panel {
        val panel = PanelListY(style)
        // hide non-connected controllers
        panel.add(SpyPanel(style) {
            panel.isVisible = controller.isConnected
        })
        panel.weight = 1f
        val list = PanelListX(style)
        panel.add(list)
        if (controller.numAxes >= 2) {
            list.add(StickPanel(controller, 0, 1, style))
        }
        if (controller.numAxes >= 4) {
            list.add(StickPanel(controller, 2, 3, style))
        }
        if (controller.numAxes >= 6) {
            list.add(TriggerPanel(controller, 4, style))
            list.add(TriggerPanel(controller, 5, style))
        }
        // how can we pair the axes? 0/1 = left x/y, 2/3 = right x/y, 4/5 = sticks
        // to do add a mapping for keys, so there is the name, you click on it, and then press the corresponding key
        panel.add(TextButton("Save", false, style).addLeftClickListener {
            val config = controller.calibration
            for (child in list.children) {
                if (child is CalibrationPanel) {
                    child.save(config)
                }
            }
            config.isCalibrated = true
            saveCalibration(controller.guid, controller.calibration)
        })
        return panel
    }

}