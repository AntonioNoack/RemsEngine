package ffmpeg.libavformat

enum class AVDiscard(val value: Int){
    /* We leave some space between them for extensions (drop some
     * keyframes for intra-only or drop just some bidir frames). */
    AVDISCARD_NONE(-16), ///< discard nothing
    AVDISCARD_DEFAULT(0), ///< discard useless packets like 0 size packets in avi
    AVDISCARD_NONREF(8), ///< discard all non reference
    AVDISCARD_BIDIR(16), ///< discard all bidirectional frames
    AVDISCARD_NONINTRA(24), ///< discard all non intra frames
    AVDISCARD_NONKEY(32), ///< discard all frames except keyframes
    AVDISCARD_ALL(48), ///< discard all
};