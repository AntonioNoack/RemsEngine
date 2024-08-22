package me.anno.tests.gfx

import me.anno.Time
import me.anno.gpu.texture.TextureCache
import me.anno.utils.OS.res

fun main() {
    testTexture("TextureMapper", false) {
        val src = if (Time.gameTime % 2.0 < 1.0) res.getChild("icon.png/bgra.png")
        else res.getChild("textures/UVChecker.png")
        TextureCache[src, false]!!
    }
}