package me.anno.ui.base

import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.image.ImageScale
import me.anno.input.Input
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.pow
import me.anno.ui.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.style.Style
import me.anno.utils.structures.tuples.IntPair
import kotlin.math.roundToInt

/**
 * Panel that draws a gpu.Texture2D, e.g. for icons, showing images, ...
 * */
@Suppress("MemberVisibilityCanBePrivate")
abstract class ImagePanel(style: Style) : Panel(style) {

    enum class StretchModes {
        OVERFLOW,
        PADDING,
        STRETCH
    }

    var stretchMode = StretchModes.STRETCH

    var showAlpha = false
    var flipX = false
    var flipY = false

    var allowMovement = false
    var allowZoom = false

    fun enableControls(enable: Boolean = true): ImagePanel {
        allowMovement = enable
        allowZoom = enable
        return this
    }

    var imageAlignmentX = AxisAlignment.CENTER
    var imageAlignmentY = AxisAlignment.CENTER

    override val canDrawOverBorders get() = true

    override fun getVisualState(): Any? = getTexture()

    abstract fun getTexture(): ITexture2D?

    // last image stats
    var lix = 0
    var liy = 0
    var liw = 1
    var lih = 1

    var offsetX = 0f
    var offsetY = 0f

    var zoom = 1f

    private fun stretch(texture: ITexture2D): IntPair {
        return when (stretchMode) {
            StretchModes.OVERFLOW -> ImageScale.scaleMin(texture.width, texture.height, w, h)
            StretchModes.PADDING -> ImageScale.scaleMax(texture.width, texture.height, w, h)
            else -> IntPair(w, h)
        }
    }

    private fun calculateSizes(texture: ITexture2D) {
        var (liw, lih) = stretch(texture)
        liw = (liw * zoom).toInt()
        lih = (lih * zoom).toInt()
        if (flipX) liw = -liw
        if (flipY) lih = -lih
        lix = x + (offsetX * zoom).roundToInt() + imageAlignmentX.getOffset(w, liw)
        liy = y + (offsetY * zoom).roundToInt() + imageAlignmentY.getOffset(h, lih)
        this.liw = liw
        this.lih = lih
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val texture = getTexture() ?: return
        calculateSizes(texture)
        if (showAlpha) DrawTextures.drawTransparentBackground(lix, liy, liw, lih)
        drawTexture(lix, liy + lih, liw, -lih, texture, -1, null)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (allowMovement && Input.mouseKeysDown.isNotEmpty()) {
            offsetX += dx / zoom
            offsetY += dy / zoom
        } else super.onMouseMoved(x, y, dx, dy)
    }

    var minZoom = 1f
    var maxZoom = 1e3f
    var zoomSpeed = 0.05f

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        val texture = getTexture()
        if (allowZoom && texture != null) {
            val (w, h) = stretch(texture)
            // calculate where the mouse is
            val mouseX0 = (x - lix) / liw * w
            val mouseY0 = (y - liy) / lih * h
            zoom = clamp(zoom * pow(1f + zoomSpeed, dy), minZoom, maxZoom)
            calculateSizes(texture)
            // calculate it again, and then remove the delta
            val mouseX1 = (x - lix) / liw * w
            val mouseY1 = (y - liy) / lih * h
            val dmx = mouseX0 - mouseX1
            val dmy = mouseY0 - mouseY1
            if (dmx.isFinite() && dmy.isFinite()) {
                offsetX -= dmx * (if (flipX) -1 else +1)
                offsetY -= dmy * (if (flipY) -1 else +1)
            }
        } else super.onMouseWheel(x, y, dx, dy, byMouse)
    }

}