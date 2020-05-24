package me.anno.config

import me.anno.config.DefaultStyle.baseTheme
import me.anno.io.config.ConfigBasics
import me.anno.io.utils.StringMap
import me.anno.ui.style.Style

object DefaultConfig: StringMap() {

    init {
        init()
    }

    lateinit var style: Style

    fun init() {

        val t0 = System.nanoTime()

        this["style"] = "dark"

        val newConfig = ConfigBasics.loadConfig("main.config", this, true)
        if(newConfig != this){
            clear()
            putAll(newConfig)
        }

        val stylePath = newConfig["style"]?.toString() ?: "dark"
        style = baseTheme.getStyle(stylePath)

        val t1 = System.nanoTime()
        println("used ${(t1-t0)*1e-9f} to read the config")

    }

}