package me.anno.tests.ui

import me.anno.animation.LoopingState
import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.input.Key
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

fun main() {
    disableRenderDoc()
    testUI3("Video Playback") {
        val list = PanelListY(style)
        val source = videos.getChild("treemiddle.mp4")
        val controls = PanelListX(style)
        controls.fill(0f)
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

            override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
                super.onMouseClicked(x, y, button, long)
                val time = (x - this.x) / width * movie.meta.videoDuration
                movie.stream.skipTo(time)
            }
        }.fill(1f))
        movie.fill(1f)
        list.add(movie)
        list.add(controls)
        list.fill(1f)
        list
    }
}