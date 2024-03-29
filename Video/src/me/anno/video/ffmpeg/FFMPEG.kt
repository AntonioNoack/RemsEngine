package me.anno.video.ffmpeg

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.utils.OS

object FFMPEG {

    // Linux needs ffmpeg to be installed; on Windows just use and call the exe files
    val isInstalled = DefaultConfig["ffmpeg.isInstalled", !OS.isWindows]

    var ffmpegPath = if (isInstalled) getReference("ffmpeg") else
        DefaultConfig["ffmpeg.path", OS.downloads.getChild("lib/ffmpeg/bin/ffmpeg.exe")]
    var ffprobePath = if (isInstalled) getReference("ffprobe") else
        DefaultConfig["ffmpeg.probe.path", ffmpegPath.getSibling("ffprobe.exe")]

    val ffmpeg
        get() = if (isInstalled) ffmpegPath else makeSureExists(ffmpegPath)
            ?: throw RuntimeException("FFmpeg not found! (path: $ffmpegPath), can't use videos, nor webp!")
    val ffprobe
        get() = if (isInstalled) ffprobePath else makeSureExists(ffprobePath)
            ?: throw RuntimeException("FFprobe not found! (path: $ffprobePath), can't use videos, nor webp!")

    fun makeSureExists(file: FileReference): FileReference? {
        return if (!file.exists || file.isDirectory) null
        else file
    }
}