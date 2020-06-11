package ffmpeg.libavformat

/**
typedef struct AVProbeData {
const char *filename;
unsigned char *buf; /**< Buffer must have AVPROBE_PADDING_SIZE of extra allocated bytes filled with zero. */
int buf_size;       /**< Size of buf except extra allocated bytes */
const char *mime_type; /**< mime_type, when known. */
} AVProbeData;
 * */
class AVProbeData(
    val filename: String,
    val buf: ByteArray,
    val buf_size: Int,
    val mime_type: String){

    companion object {

        /**
        #define AVPROBE_SCORE_RETRY (AVPROBE_SCORE_MAX/4)
        #define AVPROBE_SCORE_STREAM_RETRY (AVPROBE_SCORE_MAX/4-1)

        #define AVPROBE_SCORE_EXTENSION  50 ///< score for file extension
        #define AVPROBE_SCORE_MIME       75 ///< score for file mime type
        #define AVPROBE_SCORE_MAX       100 ///< maximum score
         * */

        const val AVPROBE_SCORE_EXTENSION = 50
        const val AVPROBE_SCORE_MIME = 75
        const val AVPROBE_SCORE_MAX = 100
        const val AVPROBE_SCORE_RETRY = AVPROBE_SCORE_MAX/4
        const val AVPROBE_SCORE_STREAM_RETRY = AVPROBE_SCORE_MAX/4-1

        const val AVPROBE_PADDING_SIZE = 32             ///< extra allocated bytes at the end of the probe buffer
        const val AVFMT_NOFILE =        0x0001
        const val AVFMT_NEEDNUMBER =    0x0002
        const val AVFMT_SHOW_IDS =      0x0008
        const val AVFMT_GLOBALHEADER =  0x0040
        const val AVFMT_NOTIMESTAMPS =  0x0080
        const val AVFMT_GENERIC_INDEX = 0x0100
        const val AVFMT_TS_DISCONT =    0x0200
        const val AVFMT_VARIABLE_FPS =  0x0400
        const val AVFMT_NODIMENSIONS =  0x0800
        const val AVFMT_NOSTREAMS    =  0x1000
        const val AVFMT_NOBINSEARCH  =  0x2000
        const val AVFMT_NOGENSEARCH  =  0x4000
        const val AVFMT_NO_BYTE_SEEK =  0x8000
        const val AVFMT_ALLOW_FLUSH  = 0x10000
        const val AVFMT_TS_NONSTRICT = 0x20000
        const val AVFMT_TS_NEGATIVE  = 0x40000
        const val AVFMT_SEEK_TO_PTS= 0x4000000

        /**
         *
        #define AVPROBE_PADDING_SIZE 32             ///< extra allocated bytes at the end of the probe buffer

        /// Demuxer will use avio_open, no opened file should be provided by the caller.
        #define AVFMT_NOFILE        0x0001
        #define AVFMT_NEEDNUMBER    0x0002 /**< Needs '%d' in filename. */
        #define AVFMT_SHOW_IDS      0x0008 /**< Show format stream IDs numbers. */
        #define AVFMT_GLOBALHEADER  0x0040 /**< Format wants global header. */
        #define AVFMT_NOTIMESTAMPS  0x0080 /**< Format does not need / have any timestamps. */
        #define AVFMT_GENERIC_INDEX 0x0100 /**< Use generic index building code. */
        #define AVFMT_TS_DISCONT    0x0200 /**< Format allows timestamp discontinuities. Note, muxers always require valid (monotone) timestamps */
        #define AVFMT_VARIABLE_FPS  0x0400 /**< Format allows variable fps. */
        #define AVFMT_NODIMENSIONS  0x0800 /**< Format does not need width/height */
        #define AVFMT_NOSTREAMS     0x1000 /**< Format does not require any streams */
        #define AVFMT_NOBINSEARCH   0x2000 /**< Format does not allow to fall back on binary search via read_timestamp */
        #define AVFMT_NOGENSEARCH   0x4000 /**< Format does not allow to fall back on generic search */
        #define AVFMT_NO_BYTE_SEEK  0x8000 /**< Format does not allow seeking by bytes */
        #define AVFMT_ALLOW_FLUSH  0x10000 /**< Format allows flushing. If not set, the muxer will not receive a NULL packet in the write_packet function. */
        #define AVFMT_TS_NONSTRICT 0x20000 /**< Format does not require strictly
        increasing timestamps, but they must
        still be monotonic */
        #define AVFMT_TS_NEGATIVE  0x40000 /**< Format allows muxing negative
        timestamps. If not set the timestamp
        will be shifted in av_write_frame and
        av_interleaved_write_frame so they
        start from 0.
        The user or muxer can override this through
        AVFormatContext.avoid_negative_ts
        */

        #define AVFMT_SEEK_TO_PTS   0x4000000 /**< Seeking is based on PTS */

         * */
    }

}