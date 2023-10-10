package me.anno

object Engine {

    @JvmStatic
    var projectName = "Rem's Engine"

    @JvmStatic
    var shutdown: Boolean = false
        private set

    private val onShutdown = ArrayList<Runnable>()
    fun registerForShutdown(callable: Runnable) {
        if (shutdown) callable.run()
        else synchronized(onShutdown) {
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
            onShutdown.clear()
        }
    }
}