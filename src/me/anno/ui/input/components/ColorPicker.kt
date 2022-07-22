package me.anno.ui.input.components

import me.anno.config.DefaultStyle
import me.anno.gpu.Cursor
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.maths.Maths
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.unmix
import me.anno.ui.base.ImagePanel
import me.anno.ui.style.Style
import me.anno.utils.Color.toHexColor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ColorPicker(
    val gpuData: Framebuffer?,
    val gpuTexture: Texture2D,
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

    override fun onUpdate() {
        invalidateDrawing()
    }

    fun getMouseCoordinates(): Pair<Int, Int> {
        val window = window!!
        var x01 = unmix(lix.toFloat(), (lix + liw).toFloat(), window.mouseX)
        var y01 = unmix(liy.toFloat(), (liy + lih).toFloat(), window.mouseY)
        if (flipX) x01 = 1f - x01
        if (flipY) y01 = 1f - y01
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
        // only show the lens, when it makes sense
        val red = 0xff0000 or DefaultStyle.black
        if (pixelScale > ceilDiv(abs(lih), max(1, gpuTexture.h))) {
            val width = pixelCount * (pixelScale + pixelSpacing) - pixelSpacing
            val totalWidth = width + generalPadding * 2
            val (mouseX, mouseY) = getMouseCoordinates()
            val window = window!!
            // draw rect there, where it isn't in the way
            val x3 = window.mouseXi - totalWidth / 2
            val y3 = window.mouseYi - totalWidth / 2
            val color = cpuData.getRGB(mouseX, mouseY)
            // show zoomed-in view with maybe 5x5 pixels
            DrawRectangles.drawRect(x3, y3, width + generalPadding * 4, width + generalPadding * 4, color)
            DrawRectangles.drawRect(x3 + generalPadding * 2, y3 + generalPadding * 2, width, width, backgroundColor)
            DrawRectangles.drawRect(
                x3 + generalPadding * 2 + (pixelCount / 2) * (pixelScale + pixelSpacing) - pixelSpacing,
                y3 + generalPadding * 2 + (pixelCount / 2) * (pixelScale + pixelSpacing) - pixelSpacing,
                pixelScale + pixelSpacing * 2,
                pixelScale + pixelSpacing * 2,
                red
            )
            for (yi in 0 until pixelCount) {
                val y4 = y3 + generalPadding * 2 + yi * (pixelScale + pixelSpacing)
                for (xi in 0 until pixelCount) {
                    val x4 = x3 + generalPadding * 2 + xi * (pixelScale + pixelSpacing)
                    val mx = mouseX + xi - pixelCount / 2
                    val my = mouseY + yi - pixelCount / 2
                    val x = Maths.clamp(mx, max(0, x0), min(x1, cpuData.width) - 1)
                    val y = Maths.clamp(my, max(0, y0), min(y1, cpuData.height) - 1)
                    DrawRectangles.drawRect(x4, y4, pixelScale, pixelScale, cpuData.getRGB(x, y))
                }
            }
        } else {
            // show red border around pixel at cursor
            var (mouseX, mouseY) = getMouseCoordinates()
            if (flipX) mouseX = gpuTexture.w - 1 - mouseX
            if (flipY) mouseY = gpuTexture.h - 1 - mouseY
            val x2 = lix + mouseX * liw / gpuTexture.w
            val y2 = liy + mouseY * lih / gpuTexture.h
            val x3 = lix + (mouseX + 1) * liw / gpuTexture.w
            val y3 = liy + (mouseY + 1) * lih / gpuTexture.h
            DrawRectangles.drawBorder(min(x2, x3), min(y2, y3), abs(x3 - x2), abs(y3 - y2), red, 2)
        }
    }

    fun sign(i: Int) = if (i < 0) -1 else +1

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
        if (ownsGPUData) {
            gpuData?.destroy()
            gpuTexture.destroy()
        }
    }

    var callback: (Int) -> Unit = {}

    override fun getCursor() = Cursor.hand
    override fun getTexture() = gpuTexture

}