package ffmpeg.libavformat

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
abstract class Demuxer(val name: String, val longName: String, val extensions: String, val flags: Int){

    abstract fun readProbe(p: AVProbeData): Int
    abstract fun readHeader(s: AVFormatContext): Int
    abstract fun readPacket(s: AVFormatContext, pkt: AVPacket): Int

    open fun readSeek(s: AVFormatContext, stream_index: Int, timestamp: Long, flags: Int): Int = 0

    fun memcmp(bytes: ByteArray, target: String, length: Int): Int = memcmp(bytes, 0, target.toByteArray(), length)
    fun memcmp(bytes: ByteArray, offset: Int, target: String, length: Int): Int = memcmp(bytes, offset, target.toByteArray(), length)

    fun memcmp(bytes: ByteArray, offset: Int, target: ByteArray, length: Int): Int {
        for((i, v) in target.withIndex()){
            if(i >= length) break
            val v0 = bytes[i+offset]
            if(v < v0) return -1
            if(v > v0) return +1
        }
        return 0
    }

}