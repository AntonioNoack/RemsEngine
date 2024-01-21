package me.anno

import me.anno.image.Image
import me.anno.images.BIImage.toImage
import me.anno.input.Output
import org.apache.logging.log4j.LogManager
import java.awt.AWTException
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit

object AWTRobot {

    @JvmField
    val robot = try {
        Robot()
    } catch (e: AWTException) {
        e.printStackTrace()
        null
    }

    private val LOGGER = LogManager.getLogger(AWTRobot::class)
    fun register() {
        Output.systemMousePressImpl = { key ->
            robot?.mousePress(Output.keyToRobot(key))
        }
        Output.systemMouseReleaseImpl = { key ->
            robot?.mouseRelease(Output.keyToRobot(key))
        }
        Output.systemMouseWheelImpl = { delta ->
            robot?.mouseWheel(delta)
        }
        Output.systemMouseMoveImpl = { window, xInWindow, yInWindow ->
            val x = window.positionX + xInWindow
            val y = window.positionY + yInWindow
            // this is broken, but I have no idea why :(
            // when I click another window via window bottom bar, I loose control over the mouse with the controller
            LOGGER.debug("Setting mouse to $x, $y")
            robot?.mouseMove(x, y)
        }
    }

    fun takeScreenshot(): Image? {
        val robot = robot ?: return null
        val image = robot.createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))
        return image.toImage()
    }
}