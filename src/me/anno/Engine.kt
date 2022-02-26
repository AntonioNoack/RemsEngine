package me.anno

import me.anno.ui.debug.FrameTimes
import kotlin.math.min

object Engine {

    var rawDeltaTime = 0f
    var deltaTime = 0f

    var currentFPS = 60f

    val startTime = System.nanoTime()
    private var lastTime = startTime

    val startDateTime = System.currentTimeMillis()

    /**
     * time at this moment in time since the engine started; in nano seconds
     * */
    val nanoTime get() = System.nanoTime() - startTime

    /**
     * time of current frame; since the engine started; in nano seconds
     * use gameTimeF for a float value
     * */
    var gameTime = lastTime - startTime
        private set

    var gameTimeF = 0.0
    private set

    var shutdown = false
        private set

    fun updateTime() {

        val thisTime = System.nanoTime()
        rawDeltaTime = (thisTime - lastTime) * 1e-9f
        deltaTime = min(rawDeltaTime, 0.1f)
        FrameTimes.putTime(rawDeltaTime)

        val newFPS = 1f / rawDeltaTime
        currentFPS = min(currentFPS + (newFPS - currentFPS) * 0.05f, newFPS)
        lastTime = thisTime

        gameTime = lastTime - startTime
        gameTimeF = gameTime.toDouble()

    }

    fun requestShutdown() {
        shutdown = true
    }

}