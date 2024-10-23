package me.anno.ui.base.image

import me.anno.gpu.texture.Filtering
import me.anno.input.Input
import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.StretchModes
import me.anno.utils.types.Floats.roundToIntOr

open class ImagePanelBase(style: Style) : Panel(style) {

    var filtering = Filtering.TRULY_LINEAR

    var stretchMode = StretchModes.PADDING

    var showAlpha = false
    var flipX = false
    var flipY = false

    var allowMovement = false
    var allowZoom = false

    fun enableControls(enable: Boolean = true): ImagePanelBase {
        allowMovement = enable
        allowZoom = enable
        return this
    }

    var imageAlignmentX = AxisAlignment.CENTER
    var imageAlignmentY = AxisAlignment.CENTER

    override val canDrawOverBorders: Boolean get() = true

    // last image stats
    var lix = 0
    var liy = 0
    var liw = 1
    var lih = 1

    var offsetX = 0f
    var offsetY = 0f

    var zoom = 1f

    var minZoom = 0.1f
    var maxZoom = 1e3f
    var zoomSpeed = 0.05f

    private var lastWidth = 10
    private var lastHeight = 10

    fun calculateSizes(srcW: Int, srcH: Int) {
        lastWidth = srcW
        lastHeight = srcH
        var (liw, lih) = stretchMode.stretch(srcW, srcH, width, height)
        liw = (liw * zoom).toInt()
        lih = (lih * zoom).toInt()
        if (flipX) liw = -liw
        if (flipY) lih = -lih
        lix = x + (offsetX * zoom).roundToIntOr() + imageAlignmentX.getOffset(width, liw)
        liy = y + (offsetY * zoom).roundToIntOr() + imageAlignmentY.getOffset(height, lih)
        this.liw = liw
        this.lih = lih
    }

    fun resetTransform(){
        offsetX = 0f
        offsetY = 0f
        zoom = 1f
        invalidateDrawing()
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (allowMovement && Input.mouseKeysDown.isNotEmpty()) {
            offsetX += dx / zoom
            offsetY += dy / zoom
            invalidateDrawing()
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        if (allowZoom) {
            val (w, h) = stretchMode.stretch(lastWidth, lastHeight, width, height)
            // calculate where the mouse is
            val mouseX0 = (x - lix) / liw * w
            val mouseY0 = (y - liy) / lih * h
            zoom = Maths.clamp(zoom * Maths.pow(1f + zoomSpeed, dy), minZoom, maxZoom)
            calculateSizes(lastWidth, lastHeight)
            // calculate it again, and then remove the delta
            val mouseX1 = (x - lix) / liw * w
            val mouseY1 = (y - liy) / lih * h
            val dmx = mouseX0 - mouseX1
            val dmy = mouseY0 - mouseY1
            if (dmx.isFinite() && dmy.isFinite()) {
                offsetX -= dmx * (if (flipX) -1 else +1)
                offsetY -= dmy * (if (flipY) -1 else +1)
            }
            invalidateDrawing()
        } else super.onMouseWheel(x, y, dx, dy, byMouse)
    }

    override fun clone(): ImagePanelBase {
        val clone = ImagePanelBase(style)
        copyInto(clone)
        return clone
    }
}