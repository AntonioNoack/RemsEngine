package me.anno

import me.anno.cache.CacheSection
import me.anno.gpu.WindowManagement
import me.anno.utils.Threads

object Engine {

    @JvmStatic
    var projectName = "Rem's Engine"

    @JvmStatic
    private var shutdownFlag: Boolean = false

    @JvmStatic
    val shutdown: Boolean
        get() = shutdownFlag || Threads.isIdleQuickCheck()

    private val onShutdown = ArrayList<() -> Unit>()
    fun registerForShutdown(callback: () -> Unit) {
        if (shutdown) callback()
        synchronized(onShutdown) {
            onShutdown.add(callback)
        }
    }

    /**
     * Explicit call to stop waiting on optional load processes, and shut down any remaining workers.
     * After calling this, lots of things will break, so don't call this unless you are sure.
     *
     * If you want to save the engine from a shutdown, call Engine.cancelShutdown(), but not everything might work again.
     * */
    @JvmStatic
    fun requestShutdown() {
        synchronized(this) {
            if (shutdownFlag) return
            shutdownFlag = true
        }
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
        shutdownFlag = false
        // how are CacheSections handling ShutdownErrors? Can a once failed resource still be created, or will it be failed?
        //  - it looks like they just keep <null> as their value... -> clear them all 😄
        CacheSection.clearAll()
        WindowManagement.destroyed = false
    }
}