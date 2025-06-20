package me.anno.ui.base.image

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.texture.Clamping
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.MediaMetadata
import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.utils.Color
import me.anno.utils.Color.withAlpha
import me.anno.video.VideoStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class VideoPanel(source: FileReference, meta: MediaMetadata, playAudio: Boolean, style: Style) :
    ImagePanelBase(style) {

    val meta = MediaMetadata.getMeta(source).waitFor()!!

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

    var stream = VideoStream(source, meta, playAudio, LoopingState.PLAY_ONCE, meta.videoFPS, -1)
        private set

    init {
        // set this to zero, so we don't request a size that we won't need
        width = 0
        height = 0
    }

    private fun reload() {
        stream.destroy()
        val meta = MediaMetadata.getMeta(source).waitFor()!!
        stream = VideoStream(source, meta, playAudio, looping, meta.videoFPS, max(width, height))
    }

    fun stop() {
        stream.stop()
        destroy()
    }

    override fun onUpdate() {
        super.onUpdate()
        requestSize(width, height)
    }

    fun requestSize(w: Int, h: Int) {
        // invalid dimensions can happen with weird layout, or before size is known
        if (w <= 0 || h <= 0) return

        val oldMax = stream.maxSize
        stream.maxSize = min(
            max(meta.videoWidth, meta.videoHeight),
            max(w, h)
        )
        // is this properly set before we start the video?
        // if this is the first time, skipTo(0.0)
        if (oldMax == -1 && stream.maxSize > 0) {
            stream.skipTo(0.0)
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        val texture = stream.getFrame() ?: return
        calculateSizes(texture.width, texture.height)
        DrawTextures.drawTexture(
            lix, liy, liw, lih, texture,
            filtering, Clamping.CLAMP, flipY
        )
    }

    override fun destroy() {
        super.destroy()
        stream.destroy()
    }

    companion object {
        fun createSimpleVideoPlayer(source: FileReference, style: Style): Panel {
            val list = PanelListY(style)
            val controls = PanelListX(style)
            val meta = MediaMetadata.getMeta(source).waitFor()!!
            val movie = object : VideoPanel(source, meta, true, style) {

                override fun onKeyTyped(x: Float, y: Float, key: Key) {
                    when (key) {
                        Key.KEY_SPACE -> stream.togglePlaying()
                        Key.KEY_PERIOD -> if (stream.isPlaying) stream.togglePlaying()
                        else stream.skipTo(max(stream.getLoopingTimeSeconds() + 1.0 / meta.videoFPS, 0.0))
                        Key.KEY_COMMA -> if (stream.isPlaying) stream.togglePlaying()
                        else stream.skipTo(
                            min(
                                stream.getLoopingTimeSeconds(),
                                meta.videoDuration
                            ) - 1.0 / meta.videoFPS
                        )
                        Key.KEY_0, Key.KEY_KP_0 -> resetTransform()
                        else -> super.onKeyTyped(x, y, key)
                    }
                }

                override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
                    if (button == Key.BUTTON_LEFT) stream.togglePlaying()
                    else super.onMouseClicked(x, y, button, long)
                }
            }
            movie.looping = LoopingState.PLAY_LOOP
            controls.add(
                TextButton(NameDesc(">"), 1.5f, style)
                    .addLeftClickListener {
                        movie.stream.togglePlaying()
                        (it as TextButton).text = if (movie.stream.isPlaying) "||" else ">"
                    })
            controls.add(object : Panel(style) {
                override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
                    super.draw(x0, y0, x1, y1)
                    val xi = (x + width * movie.stream.getLoopingTimeSeconds() / movie.meta.videoDuration).toInt()
                    DrawRectangles.drawRect(x + 2, y + height / 2 - 1, width - 4, 2, Color.white.withAlpha(127))
                    DrawRectangles.drawRect(xi - 1, y + 3, 3, height - 3, Color.white)
                }

                private var lastScrubbed = 0L

                override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                    if (Input.isLeftDown && abs(Time.nanoTime - lastScrubbed) > 50 * Maths.MILLIS_TO_NANOS &&
                        movie.stream.hasCurrentFrame()
                    ) {
                        // this may be very expensive!!!; this class isn't meant for scrubbing
                        val time = (x - this.x) / width * movie.meta.videoDuration
                        movie.stream.skipTo(time)
                        lastScrubbed = Time.nanoTime
                    } else super.onMouseMoved(x, y, dx, dy)
                }

                override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
                    super.onMouseClicked(x, y, button, long)
                    val time = (x - this.x) / width * movie.meta.videoDuration
                    movie.stream.skipTo(time)
                }
            }.fill(1f))
            movie.enableControls()
            movie.fill(1f)
            list.add(movie)
            list.add(controls)
            return list
        }
    }
}