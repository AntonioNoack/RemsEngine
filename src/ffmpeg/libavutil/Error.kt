package ffmpeg.libavutil

import ffmpeg.libavutil.Common.MKTAG

object Error {


    fun FFERRTAG(a: Char, b: Char, c: Char, d: Char) = -MKTAG(a.toInt(), b.toInt(), c.toInt(), d.toInt())
    fun FFERRTAG(a: Int, b: Char, c: Char, d: Char) = -MKTAG(a, b.toInt(), c.toInt(), d.toInt())
    fun FFERRTAG(a: Int, b: Int, c: Int, d: Int) = -MKTAG(a, b, c, d)


    val AVERROR_BSF_NOT_FOUND     = FFERRTAG(0xF8,'B','S','F') ///< Bitstream filter not found
    val AVERROR_BUG               = FFERRTAG( 'B','U','G','!') ///< Internal bug, also see AVERROR_BUG2
    val AVERROR_BUFFER_TOO_SMALL  = FFERRTAG( 'B','U','F','S') ///< Buffer too small
    val AVERROR_DECODER_NOT_FOUND = FFERRTAG(0xF8,'D','E','C') ///< Decoder not found
    val AVERROR_DEMUXER_NOT_FOUND  = FFERRTAG(0xF8,'D','E','M') ///< Demuxer not found
    val AVERROR_ENCODER_NOT_FOUND  = FFERRTAG(0xF8,'E','N','C') ///< Encoder not found
    val AVERROR_EOF                = FFERRTAG( 'E','O','F',' ') ///< End of file
    val AVERROR_EXIT               = FFERRTAG( 'E','X','I','T') ///< Immediate exit was requested; the called function should not be restarted
    val AVERROR_EXTERNAL           = FFERRTAG( 'E','X','T',' ') ///< Generic error in an external library
    val AVERROR_FILTER_NOT_FOUND   = FFERRTAG(0xF8,'F','I','L') ///< Filter not found
    val AVERROR_INVALIDDATA        = FFERRTAG( 'I','N','D','A') ///< Invalid data found when processing input
    val AVERROR_MUXER_NOT_FOUND    = FFERRTAG(0xF8,'M','U','X') ///< Muxer not found
    val AVERROR_OPTION_NOT_FOUND   = FFERRTAG(0xF8,'O','P','T') ///< Option not found
    val AVERROR_PATCHWELCOME       = FFERRTAG( 'P','A','W','E') ///< Not yet implemented in FFmpeg, patches welcome
    val AVERROR_PROTOCOL_NOT_FOUND = FFERRTAG(0xF8,'P','R','O') ///< Protocol not found

    val AVERROR_STREAM_NOT_FOUND   = FFERRTAG(0xF8,'S','T','R') ///< Stream not found

    const val AV_ERROR_MAX_STRING_SIZE = 64

}