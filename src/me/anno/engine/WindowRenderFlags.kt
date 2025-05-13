package me.anno.engine

import me.anno.Build
import me.anno.config.ConfigRef

object WindowRenderFlags {
    /**
     * If the user is in another window/program, throttle the FPS to save on resources.
     * */
    var idleFPS by ConfigRef("ui.window.idleFPS", 10)

    /**
     * FPS limit when VSync is disabled.
     * Zero disables the limit.
     * */
    var maxFPS by ConfigRef("ui.window.maxFPS", 0)

    /**
     * Prevents tearing, but also increases input-latency
     * */
    var enableVSync by ConfigRef("debug.ui.enableVsync", !Build.isDebug)

    /**
     * Whether the FPS together with a frame-time-graph shall be rendered as an overlay.
     * */
    var showFPS by ConfigRef("debug.ui.showFPS", false)

    /**
     * Whether keystrokes should be shown as an overlay, too.
     * It's called tutorial keys, because it's great when making video tutorials.
     * */
    var showTutorialKeys by ConfigRef("ui.tutorial.showKeys", true)

    fun toggleVsync() {
        enableVSync = !enableVSync
    }
}