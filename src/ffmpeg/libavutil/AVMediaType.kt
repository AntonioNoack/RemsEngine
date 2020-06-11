package ffmpeg.libavutil

enum class AVMediaType(val value: Int){
    AVMEDIA_TYPE_UNKNOWN(-1),  ///< Usually treated as AVMEDIA_TYPE_DATA
    AVMEDIA_TYPE_VIDEO(0),
    AVMEDIA_TYPE_AUDIO(1),
    AVMEDIA_TYPE_DATA(2),          ///< Opaque data information usually continuous
    AVMEDIA_TYPE_SUBTITLE(3),
    AVMEDIA_TYPE_ATTACHMENT(4),    ///< Opaque data information usually sparse
    AVMEDIA_TYPE_NB(5)
}