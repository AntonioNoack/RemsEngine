package me.anno.ui.base.groups

import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Texture2D
import me.anno.maths.Maths.min
import me.anno.ui.Style

/**
 * Makes an image / a texture zoomable for inspection by the user
 * */
abstract class ZoomableImagePanel(style: Style) : MapPanel(style) {
    abstract fun getTexture(): Texture2D?
    private var firstValidFrame = true
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val tex = getTexture() ?: return
        if (firstValidFrame) {
            scale = min(width.toDouble() / tex.width, height.toDouble() / tex.height)
            targetScale = scale
            firstValidFrame = false
        }
        val x = coordsToWindowX(0.0).toInt()
        val y = coordsToWindowY(0.0).toInt()
        val w = coordsToWindowDirX(tex.width.toDouble()).toInt()
        val h = coordsToWindowDirY(tex.height.toDouble()).toInt()
        DrawTextures.drawTexture(x - w / 2, y - h / 2, w, h, tex)
    }
}