package me.anno.ui.base

import me.anno.gpu.drawing.DrawTextures
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.StretchModes
import me.anno.video.VideoStream
import me.anno.video.ffmpeg.MediaMetadata.Companion.getMeta

open class VideoPanel(source: FileReference, playAudio: Boolean, style: Style) : Panel(style) {

    // todo option to run movie looping

    val meta = getMeta(source, false)!!
    var stream = VideoStream(source, meta, playAudio)
        private set

    var source = source
        set(value) {
            if (field != value) {
                field = value
                reload()
            }
        }

    var playAudio = playAudio
        set(value) {
            if (field != value) {
                field = value
                reload()
            }
        }

    private fun reload() {
        stream.destroy()
        stream = VideoStream(source, meta, playAudio)
    }

    var stretchMode = StretchModes.PADDING

    override val canDrawOverBorders: Boolean
        get() = true

    override fun onUpdate() {
        super.onUpdate()
        invalidateDrawing()
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val texture = stream.getFrame() ?: return
        val (w, h) = stretchMode.stretch(texture.width, texture.height, width, height)
        DrawTextures.drawTexture(x + (width - w) / 2, y + (height - h) / 2, w, h, texture)
    }
}