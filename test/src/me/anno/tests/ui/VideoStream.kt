package me.anno.tests.ui

import me.anno.Time
import me.anno.animation.LoopingState
import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.ui.Panel
import me.anno.ui.base.VideoPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.videos
import me.anno.video.ffmpeg.MediaMetadata.Companion.getMeta
import kotlin.math.abs

fun main() {
    disableRenderDoc()
    testUI3("Video Playback") {
        val list = PanelListY(style)
        val source = videos.getChild("treemiddle.mp4")
        val controls = PanelListX(style)
        val movie = VideoPanel(source, getMeta(source, false)!!, true, style)
        movie.looping = LoopingState.PLAY_LOOP
        controls.add(TextButton(">", 1.5f, style)
            .addLeftClickListener {
                movie.stream.togglePlaying()
                (it as TextButton).text = if (movie.stream.isPlaying) "||" else ">"
            })
        controls.add(object : Panel(style) {
            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.onDraw(x0, y0, x1, y1)
                val xi = (x + width * movie.stream.getTime() / movie.meta.videoDuration).toInt()
                drawRect(x + 2, y + height / 2 - 1, width - 4, 2, white.withAlpha(127))
                drawRect(xi - 1, y + 3, 3, height - 3, white)
            }

            private var lastScrubbed = 0L

            override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                if (Input.isLeftDown && abs(Time.nanoTime - lastScrubbed) > 50 * MILLIS_TO_NANOS &&
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
        list.fill(1f)
        list
    }
}