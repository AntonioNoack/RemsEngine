package me.anno.ui.input.components

import me.anno.config.DefaultStyle
import me.anno.gpu.Cursor
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.image.Image
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.maths.Maths
import me.anno.maths.Maths.unmix
import me.anno.ui.base.ImagePanel
import me.anno.ui.style.Style
import me.anno.utils.Color.toHexColor
import kotlin.math.max
import kotlin.math.min

class ColorPicker(
    val gpuData: Framebuffer,
    val cpuData: Image,
    val ownsGPUData: Boolean,
    style: Style
) : ImagePanel(style) {

    // todo bug: when zoomed in (after window size change), the color preview is incorrect... why?

    init {
        // padding helps nobody, but overflow should help to find pixels within the center better :)
        stretchMode = StretchModes.OVERFLOW
        flipY = true
    }

    override fun tickUpdate() {
        invalidateDrawing()
    }

    fun getMouseCoordinates(): Pair<Int, Int> {
        val window = window!!
        val x01 = 0f + unmix(lix.toFloat(), (lix + liw).toFloat(), window.mouseX)
        val y01 = 1f - unmix(liy.toFloat(), (liy + lih).toFloat(), window.mouseY) // flip-y
        var x0w = x01 * cpuData.width
        var y0h = y01 * cpuData.height
        if (!x0w.isFinite()) x0w = 0f
        if (!y0h.isFinite()) y0h = 0f
        val mouseX = Maths.clamp(x0w.toInt(), 0, cpuData.width - 1)
        val mouseY = Maths.clamp(y0h.toInt(), 0, cpuData.height - 1)
        return Pair(mouseX, mouseY)
    }

    var pixelCount = 9 // should be odd
    var pixelScale = 6
    var pixelSpacing = 1
    var generalPadding = 2

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val width = pixelCount * (pixelScale + pixelSpacing) - pixelSpacing
        val totalWidth = width + generalPadding * 2
        val (mouseX, mouseY) = getMouseCoordinates()
        val window = window!!
        // draw rect there, where it isn't in the way
        val x3 = Maths.clamp(window.mouseXi - totalWidth / 2, x0 + generalPadding, x1 - totalWidth - generalPadding)
        val y3 = Maths.clamp(window.mouseYi - totalWidth / 2, y0 + generalPadding, y1 - totalWidth - generalPadding)
        val color = cpuData.getRGB(mouseX, mouseY)
        // show zoomed-in view with maybe 5x5 pixels
        DrawRectangles.drawRect(x3, y3, width + generalPadding * 4, width + generalPadding * 4, color)
        DrawRectangles.drawRect(x3 + generalPadding * 2, y3 + generalPadding * 2, width, width, backgroundColor)
        val cx = Maths.clamp(mouseX, pixelCount / 2, cpuData.width - 1 - pixelCount / 2)
        val cy = Maths.clamp(mouseY, pixelCount / 2, cpuData.height - 1 - pixelCount / 2)
        DrawRectangles.drawRect(
            x3 + generalPadding * 2 + ((mouseX - cx) + pixelCount / 2) * (pixelScale + pixelSpacing) - pixelSpacing,
            y3 + generalPadding * 2 + ((mouseY - cy) + pixelCount / 2) * (pixelScale + pixelSpacing) - pixelSpacing,
            pixelScale + pixelSpacing * 2,
            pixelScale + pixelSpacing * 2,
            0xff0000 or DefaultStyle.black
        )
        for (yi in 0 until pixelCount) {
            val y4 = y3 + generalPadding * 2 + yi * (pixelScale + pixelSpacing)
            for (xi in 0 until pixelCount) {
                val x4 = x3 + generalPadding * 2 + xi * (pixelScale + pixelSpacing)
                val mx = cx + xi - pixelCount / 2
                val my = cy + yi - pixelCount / 2
                val x = Maths.clamp(mx, max(0, x0), min(x1, cpuData.width) - 1)
                val y = Maths.clamp(my, max(0, y0), min(y1, cpuData.height) - 1)
                DrawRectangles.drawRect(x4, y4, pixelScale, pixelScale, cpuData.getRGB(x, y))
            }
        }
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        // get pixel color
        val (mouseX, mouseY) = getMouseCoordinates()
        val color = cpuData.getRGB(mouseX, mouseY)
        // place color into this view
        callback(color)
        // place color into clipboard
        Input.setClipboardContent(color.toHexColor())
        // hide window
        windowStack.remove(window!!)
        if(ownsGPUData) gpuData.destroy()
    }

    var callback: (Int) -> Unit = {}

    override fun getCursor() = Cursor.hand
    override fun getTexture() = gpuData.getColor0()

}