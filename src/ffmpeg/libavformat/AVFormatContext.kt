package ffmpeg.libavformat

import java.io.InputStream

class AVFormatContext {
    val streams = ArrayList<AVStream>()
    lateinit var pb: InputStream
}