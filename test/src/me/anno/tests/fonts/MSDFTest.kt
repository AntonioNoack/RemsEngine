package me.anno.tests.fonts

import me.anno.gpu.texture.TextureCache
import me.anno.input.Input
import me.anno.ui.canvas.CanvasAtlasCache.getBounds
import me.anno.ui.canvas.CanvasTextDrawHelper
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.OS.documents

fun main() {
    testDrawing("MultiSDFText") { p, canvas ->
        val texture =
            TextureCache[documents.getChild("Satiplanner/msdf-atlas-gen-1.4-win64/msdf-atlas-gen/VerdanaMSDF.png")].value
        if (texture != null && texture.isCreated()) {
            val bounds = getBounds(canvas, texture)!!
            val x = p.x
            val y = p.y
            val s = if (Input.isShiftDown) 2 else 1
            val k = if (Input.isControlDown) 1 else 2
            val w = texture.width * s / k
            val h = texture.height * s / k
            CanvasTextDrawHelper.color = -1
            CanvasTextDrawHelper.bgColor = (-1) shl 24
            canvas.pushMSDFText(bounds, x, y, w, h)
        }
    }
}