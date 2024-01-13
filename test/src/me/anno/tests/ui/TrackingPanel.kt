package me.anno.tests.ui

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ui.UIColors
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.debug.TrackingPanel
import me.anno.utils.Color.white
import kotlin.math.sin

fun main() {
    testUI3("Tracking") { // in lambda, so first value isn't that far off
        TrackingPanel(listOf(
            { sin(Time.nanoTime / 1e9) },
            { sin(Time.gameTime - 0.01) },
            { Time.rawDeltaTime }
        ), intArrayOf(white, UIColors.dodgerBlue, UIColors.darkOrange), style)
    }
}