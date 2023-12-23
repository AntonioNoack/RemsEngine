package me.anno

import me.anno.gpu.GFXBase

object Engine {

    @JvmStatic
    var projectName = "Rem's Engine"

    @JvmStatic
    var shutdown: Boolean = false
        private set

    private val onShutdown = ArrayList<Runnable>()
    fun registerForShutdown(callable: Runnable) {
        if (shutdown) callable.run()
        synchronized(onShutdown) {
            onShutdown.add(callable)
        }
    }

    @JvmStatic
    fun requestShutdown() {
        shutdown = true
        synchronized(onShutdown) {
            for (runnable in onShutdown) {
                try {
                    runnable.run()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    @JvmStatic
    @Deprecated("This is experimental!")
    fun cancelShutdown() {
        shutdown = false
        // todo how are CacheSections handling ShutdownErrors? Can a once failed resource still be created, or will it be failed?
        GFXBase.destroyed = false
    }
}