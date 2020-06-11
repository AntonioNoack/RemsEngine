package ffmpeg.libavformat.demuxers

import ffmpeg.libavformat.*
import ffmpeg.libavformat.AVProbeData.Companion.AVFMT_GENERIC_INDEX
import ffmpeg.libavutil.AVClass
import ffmpeg.libavutil.Log.AV_LOG_WARNING
import ffmpeg.libavutil.Log.av_log

object MP3: Demuxer("mp3", "MP2/3 (MPEG audio layer 2/3)", "mp2,mp3,m2a,mpa", AVFMT_GENERIC_INDEX) {
    
    val demuxer_class = AVClass("mp3")

    override fun readHeader(s: AVFormatContext): Int {
        TODO("Not yet implemented")
    }

    override fun readPacket(s: AVFormatContext, pkt: AVPacket): Int {
        TODO("Not yet implemented")
    }

    override fun readProbe(p: AVProbeData): Int {
        TODO("Not yet implemented")
    }

    
}