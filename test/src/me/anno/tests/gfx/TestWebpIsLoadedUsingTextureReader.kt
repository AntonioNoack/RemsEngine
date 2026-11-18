package me.anno.tests.gfx

import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.OS.pictures

fun main() {
    val src = pictures.getChild("Anime/6669cf6b4e6c44e-a-nw-p.webp")
    testDrawing("WebpLoading") { p, canvas ->
        val tex = TextureCache[src].value ?: TextureLib.whiteTexture
        canvas.drawTexture(p.x, p.y, p.width, p.height, tex)
    }
}