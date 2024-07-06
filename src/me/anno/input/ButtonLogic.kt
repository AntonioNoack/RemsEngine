package me.anno.input

import me.anno.config.DefaultConfig
import kotlin.math.max

object ButtonLogic {

    // should get a place in the config
    private val initialTypeDelayNanos get() = DefaultConfig["controller.initialTypeDelayMillis", 1000] * 1_000_000L
    private val typeDelayNanos get() = DefaultConfig["controller.typeDelayMillis", 100] * 1_000_000L

    const val DOWN = 1
    const val TYPE = 2
    const val UP = 4

    fun process(time: Long, pressed: Boolean, buttonDownTime: LongArray, buttonId: Int): Int {
        if (pressed) {
            if (buttonDownTime[buttonId] == 0L) {
                buttonDownTime[buttonId] = time
                return DOWN or TYPE
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
        return 0
    }
}