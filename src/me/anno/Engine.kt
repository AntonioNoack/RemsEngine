package me.anno

import me.anno.cache.CacheSection
import me.anno.gpu.GFXBase

object Engine {

    @JvmStatic
    var projectName = "Rem's Engine"

    @JvmStatic
    var shutdown: Boolean = false
        private set

    private val onShutdown = ArrayList<() -> Unit>()
    fun registerForShutdown(callback: () -> Unit) {
        if (shutdown) callback()
        synchronized(onShutdown) {
            onShutdown.add(callback)
        }
    }

    @JvmStatic
    fun requestShutdown() {
        shutdown = true
        for (i in onShutdown.indices) {
            try {
                onShutdown[i]()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    @Deprecated("This is experimental!")
    fun cancelShutdown() {
        shutdown = false
        // how are CacheSections handling ShutdownErrors? Can a once failed resource still be created, or will it be failed?
        //  - it looks like they just keep <null> as their value... -> clear them all 😄
        CacheSection.clearAll()
        GFXBase.destroyed = false
    }
}