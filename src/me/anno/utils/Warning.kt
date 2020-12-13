package me.anno.utils

object Warning {
    val warned = HashSet<String>()
    fun warn(key: String){
        if(key in warned) return
        warned += key
        println(key)
    }
}