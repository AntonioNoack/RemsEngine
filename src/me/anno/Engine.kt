package me.anno

object Engine {
    var shutdown = false
    fun shutdown(){
        shutdown = true
    }
}