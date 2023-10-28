package me.anno.tests.gfx

import me.anno.image.ImageGPUCache
import me.anno.io.files.FileReference.Companion.getReference
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

fun main() {
    LogManager.define("TextureMapper", Level.ALL)
    testTexture("TextureMapper", false) {
        ImageGPUCache[getReference("res://icon.png/bgra.png"), false]!!
    }
}