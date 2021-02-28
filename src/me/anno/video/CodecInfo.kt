package me.anno.video

// https://trac.ffmpeg.org/wiki/Encode/HighQualityAudio
object CodecInfo {

    /**
    Dolby Digital: ac3
    Dolby Digital Plus: eac3
    MP2: libtwolame, mp2
    Windows Media Audio 1: wmav1
    Windows Media Audio 2: wmav2
    AAC LC: libfdk_aac, aac
    HE-AAC: libfdk_aac
    Vorbis: libvorbis, vorbis
    MP3: libmp3lame, libshine
    Opus: libopus
     * */

    // libopus > libvorbis >= libfdk_aac > libmp3lame >= eac3/ac3 > aac > libtwolame > vorbis > mp2 > wmav2/wmav1

    val containerFormats: List<Pair<List<String>, List<String>>> = listOf(
        listOf("mkv", "mka") to listOf("opus", "vorbis", "mp2", "mp3", "lc-acc", "he-acc", "ac3", "eac3"),
        listOf("mp4", "m4a") to listOf("mp2", "mp3", "lc-acc", "he-acc", "ac3"),
        listOf("flv", "f4v") to listOf("mp3", "lc-acc", "he-acc"),
        listOf("3gp", "3g2") to listOf("lc-acc", "he-acc"),
        listOf("mpg") to listOf("mp2", "mp3"),
        listOf("ps/ts") to listOf("mp2", "mp3", "lc-acc", "he-acc", "ac3"),
        listOf("m2ts") to listOf("ac3", "eac3"),
        listOf("vob") to listOf("mp2", "ac3"),
        listOf("rmvb") to listOf("vorbis", "he-acc"),
        listOf("webm") to listOf("vorbis", "opus"),
        listOf("ogg") to listOf("vorbis", "opus")
    )

}