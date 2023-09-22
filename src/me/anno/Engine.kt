package me.anno

object Engine {

    @JvmStatic
    var projectName = "Rem's Engine"

    @JvmStatic
    var shutdown: Boolean = false
        private set

    @JvmStatic
    fun requestShutdown() {
        shutdown = true
        try {
            javaClass.classLoader.loadClass("pl.edu.icm.jlargearrays.ConcurrencyUtils")
                .getMethod("shutdownThreadPoolAndAwaitTermination")
                .invoke(null)
        } catch (ignored: Exception) {
        }
    }
}