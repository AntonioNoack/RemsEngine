package me.anno

import me.anno.ui.debug.FrameTimings
import kotlin.math.min

// todo compare engine size compiled without and with "inline"

object Engine {

    var projectName = "RemsEngine"

    var rawDeltaTime = 0f
    var deltaTime = 0f

    var currentFPS = 60f

    val startTime = System.nanoTime()
    private var lastTime = startTime

    val startDateTime = System.currentTimeMillis()

    /**
     * time at this moment since the engine started; in nanoseconds
     * */
    val nanoTime get() = System.nanoTime() - startTime

    /**
     * time of current frame; since the engine started; in nanoseconds
     * use gameTimeF for a float value
     * */
    var gameTime = lastTime - startTime
        private set

    var gameTimeF = 0f
        private set

    var gameTimeD = 0.0
        private set

    var frameIndex = 0
        private set

    var shutdown = false
        private set

    fun updateTime() {

        val thisTime = System.nanoTime()
        rawDeltaTime = (thisTime - lastTime) * 1e-9f
        deltaTime = min(rawDeltaTime, 0.1f)
        FrameTimings.putTime(rawDeltaTime)

        val newFPS = 1f / rawDeltaTime
        currentFPS = min(currentFPS + (newFPS - currentFPS) * 0.05f, newFPS)
        lastTime = thisTime

        gameTime = lastTime - startTime
        gameTimeF = gameTime * 1e-9f
        gameTimeD = gameTime * 1e-9

        frameIndex++

    }

    fun requestShutdown() {
        shutdown = true
    }

}