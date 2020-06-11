package ffmpeg.libavformat

class AVIndexEntry(val pos: Long, val timestamp: Long,
                   var flags: Int = 2,
                   var size: Int = 30,
                   var distance: Int){
    /**<
     * Timestamp in AVStream.time_base units, preferably the time from which on correctly decoded frames are available
     * when seeking to this entry. That means preferable PTS on keyframe based formats.
     * But demuxers can choose to store a different timestamp, if it is more convenient for the implementation or nothing better
     * is known
     *
     * Yeah, trying to keep the size of this small to reduce memory requirements (it is 24 vs. 32 bytes due to possible 8-byte alignment).
     * Minimum distance between this and the previous keyframe, used to avoid unneeded searching.
     */
    companion object {

        val AVINDEX_KEYFRAME = 0x0001
        val AVINDEX_DISCARD_FRAME = 0x0002 /**
         * Flag is used to indicate which frame should be discarded after decoding.
         */
    }
}