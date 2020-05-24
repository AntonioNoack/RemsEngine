package me.anno.io

import me.anno.io.config.ConfigBasics
import me.anno.io.utils.StringMap

fun main(){
    val configDefaults = StringMap()
    configDefaults["key.a"] = 17
    configDefaults["key.float"] = 11f/3f
    configDefaults["key.double"] = 11.0/3.0
    configDefaults["key.string"] = "yes!"
    configDefaults["key.config"] = StringMap()
    val config = ConfigBasics.loadConfig("test.config", configDefaults, true)
    println("loaded: $config")
}