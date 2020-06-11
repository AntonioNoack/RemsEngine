package ffmpeg.libavformat.demuxers

import ffmpeg.libavformat.AVProbeData
import ffmpeg.libavformat.AVFormatContext
import ffmpeg.libavformat.AVPacket
import ffmpeg.libavformat.AVProbeData.Companion.AVFMT_GENERIC_INDEX
import ffmpeg.libavformat.AVProbeData.Companion.AVPROBE_SCORE_MAX
import ffmpeg.libavformat.Demuxer

/**
AVInputFormat ff_threedostr_demuxer = {
.name           = "3dostr",
.long_name      = NULL_IF_CONFIG_SMALL("3DO STR"),
.read_probe     = threedostr_probe,
.read_header    = threedostr_read_header,
.read_packet    = threedostr_read_packet,
.extensions     = "str",
.flags          = AVFMT_GENERIC_INDEX,
};
 * */

object ThreeDoStr: Demuxer("3dostr", "3DO STR", "str", AVFMT_GENERIC_INDEX){

    override fun readProbe(p: AVProbeData): Int {
        if(
            memcmp(p.buf, "CTRL", 4) != 0 &&
            memcmp(p.buf, "SHDR", 4) != 0 &&
            memcmp(p.buf, "SNDS", 4) != 0)
        return 0;

        return AVPROBE_SCORE_MAX / 3 * 2;
    }

    override fun readHeader(s: AVFormatContext): Int {
        TODO("Not yet implemented")
    }

    override fun readPacket(s: AVFormatContext, pkt: AVPacket): Int {
        TODO("Not yet implemented")
    }


}