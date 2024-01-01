package me.anno.ui.base

import me.anno.animation.LoopingState
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.io.files.FileReference
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.StretchModes
import me.anno.video.VideoStream
import me.anno.video.ffmpeg.MediaMetadata
import me.anno.video.ffmpeg.MediaMetadata.Companion.getMeta

// todo only request a resolution that we need
open class VideoPanel(source: FileReference, meta: MediaMetadata, playAudio: Boolean, style: Style) : Panel(style) {

    val meta = getMeta(source, false)!!

    var looping: LoopingState
        get() = stream.looping
        set(value) {
            if (stream.looping != value) {
                stream.looping = value
                // we could skip reloading, if we're setting start -> loop & just starting...
                if (stream.isPlaying) {
                    reload()
                }
            }
        }

    var source = source
        set(value) {
            if (field != value) {
                field = value
                reload()
            }
        }

    var playAudio: Boolean
        get() = stream.playAudio
        set(value) {
            if (stream.playAudio != value) {
                stream.playAudio = value
                if (stream.isPlaying) {
                    if (value) stream.startAudio()
                    else stream.stopAudio()
                }
            }
        }

    var stream = VideoStream(source, meta, playAudio, LoopingState.PLAY_ONCE)
        private set

    private fun reload() {
        stream.destroy()
        stream = VideoStream(source, getMeta(source, false)!!, playAudio, looping)
    }

    var stretchMode = StretchModes.PADDING

    override val canDrawOverBorders: Boolean
        get() = stretchMode == StretchModes.OVERFLOW

    override fun onUpdate() {
        super.onUpdate()
        invalidateDrawing()
    }

    var filtering = Filtering.TRULY_LINEAR

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        val texture = stream.getFrame() ?: return
        val (w, h) = stretchMode.stretch(texture.width, texture.height, width, height)
        DrawTextures.drawTexture(
            x + (width - w) / 2, y + (height - h) / 2, w, h, texture,
            filtering, Clamping.CLAMP
        )
    }
}