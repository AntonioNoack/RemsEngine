package me.anno.video

import me.anno.video.CodecInfo.audioCodecList
import java.util.*

object Codecs {

    fun audioCodecByExtension(extension: String): String? {
        val lc = extension.lowercase(Locale.getDefault())
        for ((key, value) in audioCodecList) {
            if (lc in key && value.isNotEmpty()) {
                return value[0]
            }
        }
        return when (lc) {
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