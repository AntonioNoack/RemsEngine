package me.anno.video

import me.anno.video.CodecInfo.audioCodecList

object Codecs {

    fun audioCodecByExtension(lcExtension: String): String? {
        for ((key, value) in audioCodecList) {
            if (lcExtension in key && value.isNotEmpty()) {
                return value[0]
            }
        }
        return when (lcExtension) {
            "mp3", "mpg",
            "mp4", "m4a",
            "flv", "f4v" -> "libmp3lame"
            "ogg", "oga",
            "mkv", "mka",
            "webm" -> "libvorbis"
            "gif" -> null
            else -> "aac"
        }
    }

    // interesting: https://developer.mozilla.org/en-US/docs/Web/Media/Formats/Video_codecs
    fun videoCodecByExtension(extension: String): String? {
        return when (extension) {
            "gif" -> null
            "webm" -> "libvpx" // vp8=libvpx
            else -> "libx264"
        }
    }


}