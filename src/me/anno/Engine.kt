package me.anno

object Engine {

    var shutdown = false
        private set

    fun requestShutdown() {
        shutdown = true
    }

}