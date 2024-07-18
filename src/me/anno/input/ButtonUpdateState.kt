package me.anno.input

import me.anno.config.DefaultConfig
import me.anno.gpu.OSWindow
import kotlin.math.max

enum class ButtonUpdateState {

    DOWN,
    TYPE,
    UP,

    NOTHING;

    companion object {

        fun updateButtonState(time: Long, pressed: Boolean, buttonDownTime: LongArray, buttonId: Int): ButtonUpdateState {
            if (pressed) {
                if (buttonDownTime[buttonId] == 0L) {
                    buttonDownTime[buttonId] = time
                    return DOWN
                }
                val timeSinceDown = time - buttonDownTime[buttonId]
                if (timeSinceDown > initialTypeDelayNanos) {
                    buttonDownTime[buttonId] = max(
                        buttonDownTime[buttonId] + typeDelayNanos, // reset
                        time - initialTypeDelayNanos - typeDelayNanos * 2 // & we must not collect too many,
                        // when the window is not active
                    )
                    return TYPE
                }
            } else {
                if (buttonDownTime[buttonId] != 0L) {
                    buttonDownTime[buttonId] = 0L
                    return UP
                }
            }
            return NOTHING
        }

        fun callButtonUpdateEvents(
            window: OSWindow, time: Long, pressed: Boolean,
            buttonDownTime: LongArray, buttonId: Int, button: Key
        ) {
            when (updateButtonState(time, pressed, buttonDownTime, buttonId)) {
                DOWN -> Input.onKeyPressed(window, button, time)
                TYPE -> Input.onKeyTyped(window, button)
                UP -> Input.onKeyReleased(window, button)
                else -> {}
            }
        }

        // should get a place in the config
        private val initialTypeDelayNanos get() = DefaultConfig["controller.initialTypeDelayMillis", 1000] * 1_000_000L
        private val typeDelayNanos get() = DefaultConfig["controller.typeDelayMillis", 100] * 1_000_000L
    }
}