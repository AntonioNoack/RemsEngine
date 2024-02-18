package me.anno.input

import me.anno.gpu.OSWindow
import me.anno.utils.InternalAPI

object Output {

    fun keyToRobot(key: Key): Int {
        return when (key) {
            Key.BUTTON_LEFT -> 16 // InputEvent.BUTTON1_MASK
            Key.BUTTON_RIGHT -> 8 // InputEvent.BUTTON2_MASK
            else -> 4 // InputEvent.BUTTON3_MASK
        }
    }

    @InternalAPI
    var systemMousePressImpl: ((Key) -> Unit)? = null
    fun systemMousePress(key: Key) {
        systemMousePressImpl?.invoke(key)
    }

    @InternalAPI
    var systemMouseReleaseImpl: ((Key) -> Unit)? = null
    fun systemMouseRelease(key: Key) {
        systemMouseReleaseImpl?.invoke(key)
    }

    @InternalAPI
    var systemMouseWheelImpl: ((Int) -> Unit)? = null
    fun systemMouseWheel(delta: Int) {
        systemMouseWheelImpl?.invoke(delta)
    }

    @InternalAPI
    var systemMouseMoveImpl: ((OSWindow, Int, Int) -> Unit)? = null
    fun systemMouseMove(window: OSWindow, xInWindow: Int, yInWindow: Int) {
        systemMouseMoveImpl?.invoke(window, xInWindow, yInWindow)
    }
}