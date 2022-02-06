package me.anno.video.ffmpeg

object IsFFMPEGOnly {

    fun String.isFFMPEGOnlyExtension() = equals("webp", true)// || equals("jp2", true)

}