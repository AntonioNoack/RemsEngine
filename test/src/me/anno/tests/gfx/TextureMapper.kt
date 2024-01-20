package me.anno.tests.gfx

import me.anno.gpu.texture.TextureCache
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS.pictures
import org.apache.logging.log4j.LogManager

fun main() {
    LogManager.logAll()
    testTexture("TextureMapper", false) {
        TextureCache[getReference("res://icon.png/bgra.png"), false]!!
        TextureCache[pictures.getChild("Anime/0c7aceb27784c1edd0bcc4f47c66a9d0.webp"), false]!!
    }
}