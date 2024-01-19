package me.anno.video.ffmpeg

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.utils.OS

object FFMPEG {

    // Linux needs ffmpeg to be installed; on Windows just use and call the exe files
    val isInstalled = DefaultConfig["ffmpeg.isInstalled", !OS.isWindows]

    var ffmpegPath = if (isInstalled) FileReference.getReference("ffmpeg") else
        DefaultConfig["ffmpeg.path", FileReference.getReference(OS.downloads, "lib/ffmpeg/bin/ffmpeg.exe")]
    var ffprobePath = if (isInstalled) FileReference.getReference("ffprobe") else
        DefaultConfig["ffmpeg.probe.path", FileReference.getReference(ffmpegPath.getParent(), "ffprobe.exe")]

    var ffmpegPathString = if (isInstalled) "ffmpeg" else
        FileReference.getReference(DefaultConfig["ffmpeg.path", "lib/ffmpeg/ffmpeg.exe"])
            .absolutePath.replace('\\', '/')
    var ffprobePathString = if (isInstalled) "ffprobe" else
        DefaultConfig["ffmpeg.probe.path", FileReference.getReference(ffmpegPath.getParent(), "ffprobe.exe")]
            .absolutePath.replace('\\', '/')

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