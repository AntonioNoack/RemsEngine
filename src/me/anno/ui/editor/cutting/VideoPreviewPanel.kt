package me.anno.ui.editor.cutting

import me.anno.gpu.GFXx2D
import me.anno.input.Input.mouseX
import me.anno.objects.Video
import me.anno.ui.base.Panel
import me.anno.ui.style.Style
import org.joml.Vector4f

// todo why is this flickering, when moving the mouse???...
class VideoPreviewPanel(val video: Video, val height: Int, style: Style, val getTime: (x: Float) -> Double) :
    Panel(style) {

    val width = height * video.w / video.h

    init { backgroundColor = 0xff777777.toInt() }

    override val onMovementHideTooltip: Boolean = false

    override fun calculateSize(w: Int, h: Int) {
        minW = width
        minH = height
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val meta = video.meta ?: return
        val time = getTime(mouseX)
        val frame = video.getFrameAtLocalTime(time, width, meta) ?: return
        GFXx2D.drawRectGradient(
            x0, y0, x1 - x0, y1 - y0, color, color, frame,
            Vector4f(0f, 0f, 1f, 1f)
        )
    }

    companion object {
        private val color = Vector4f(1f)
    }

}