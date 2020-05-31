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
        this["ffmpegPath"] = "C:\\Users\\Antonio\\Downloads\\lib\\ffmpeg\\bin\\ffmpeg.exe"
        this["tooltip.reactionTime"] = 300
        this["lastUsed.fonts.count"] = 5

        addImportMappings("Image", "png", "jpg", "jpeg", "tiff", "webp")
        addImportMappings("Video", "mp4", "gif", "mpeg", "avi")
        addImportMappings("Text", "txt")
        addImportMappings("Markdown", "md")
        addImportMappings("Audio", "mp3", "wav", "ogg")

        this["import.mapping.*"] = "Text"

        val newConfig = ConfigBasics.loadConfig("main.config", this, true)
        if(newConfig != this){
            putAll(newConfig)
        }

        val stylePath = newConfig["style"]?.toString() ?: "dark"
        style = baseTheme.getStyle(stylePath)

        val t1 = System.nanoTime()
        println("used ${(t1-t0)*1e-9f} to read the config")

    }

    fun addImportMappings(result: String, vararg extensions: String){
        for(extension in extensions){
            this["import.mapping.$extension"] = result
        }
    }

    val defaultFont get() = this["defaultFont"] as? String ?: "Verdana"

}