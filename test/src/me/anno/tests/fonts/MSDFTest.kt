package me.anno.tests.fonts

import me.anno.gpu.texture.TextureCache
import me.anno.ui.canvas.Bounds
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
            val s = 3//if (Input.isShiftDown) 2 else 1
            val k = 1//if (Input.isControlDown) 1 else 2
            val w = 26 * s / k
            val h = 36 * s / k
            CanvasTextDrawHelper.color = -1
            CanvasTextDrawHelper.bgColor = (-1) shl 24
            // todo 26x36 is the first thing to draw
            canvas.pushMSDFText(
                Bounds(
                    bounds.x0,
                    (bounds.y1 - 36).toShort(),
                    (bounds.x0 + 26).toShort(),
                    bounds.y1,
                ), x, y, w, h
            )
        }
    }
}