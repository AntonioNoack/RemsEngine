package me.anno

import me.anno.ui.debug.FrameTimings
import kotlin.math.min

object Engine {

    @JvmField
    var projectName = "RemsEngine"

    @JvmField
    var rawDeltaTime = 0f

    @JvmField
    var deltaTime = 0f

    @JvmField
    var currentFPS = 60f

    @JvmField
    val startTime = System.nanoTime()

    @JvmStatic
    private var lastTime = startTime

    @JvmField
    val startDateTime = System.currentTimeMillis()

    /**
     * time at this moment since the engine started; in nanoseconds
     * */
    @JvmStatic
    val nanoTime get(): Long = System.nanoTime() - startTime

    /**
     * time of current frame; since the engine started; in nanoseconds
     * use gameTimeF for a float value
     * */
    @JvmStatic
    var gameTime: Long = lastTime - startTime
        private set

    @JvmStatic
    var lastGameTime: Long = 0L
        private set

    @JvmStatic
    var gameTimeF: Float = 0f
        private set

    @JvmStatic
    var gameTimeD: Double = 0.0
        private set

    @JvmStatic
    var frameIndex: Int = 0
        private set

    @JvmStatic
    var shutdown: Boolean = false
        private set

    @JvmStatic
    fun updateTime() {
        val thisTime = System.nanoTime()
        val rawDeltaTime = (thisTime - lastTime) * 1e-9f
        updateTime(rawDeltaTime, thisTime)
    }

    @JvmStatic
    fun updateTime(dt: Float, thisTime: Long) {

        rawDeltaTime = dt
        deltaTime = min(rawDeltaTime, 0.1f)
        FrameTimings.putTime(rawDeltaTime)

        val newFPS = 1f / rawDeltaTime
        currentFPS = min(currentFPS + (newFPS - currentFPS) * 0.05f, newFPS)
        lastTime = thisTime

        lastGameTime = gameTime
        gameTime = thisTime - startTime
        gameTimeF = gameTime * 1e-9f
        gameTimeD = gameTime * 1e-9

        frameIndex++

    }

    @JvmStatic
    fun requestShutdown() {
        shutdown = true

        try {
            javaClass.classLoader.loadClass("pl.edu.icm.jlargearrays.ConcurrencyUtils")
                .getMethod("shutdownThreadPoolAndAwaitTermination")
                .invoke(null)
        } catch (ignored: Exception) { }

    }

}