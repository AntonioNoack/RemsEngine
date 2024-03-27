package me.anno.tests.gfx

import me.anno.Time
import me.anno.gpu.texture.TextureCache
import me.anno.io.files.Reference.getReference

fun main() {
    testTexture("TextureMapper", false) {
        val src = if (Time.gameTime % 2.0 < 1.0) getReference("res://icon.png/bgra.png")
        else getReference("res://textures/UVChecker.png")
        TextureCache[src, false]!!
    }
}