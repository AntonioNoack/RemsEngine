package me.anno.video

import me.anno.config.DefaultConfig
import me.anno.utils.OS
import java.io.File
import java.lang.RuntimeException

object FFMPEG {

    // Linux needs ffmpeg to be installed; on Windows just use and call the exe files
    val isInstalled = DefaultConfig["ffmpeg.isInstalled", !OS.isWindows]

    var ffmpegPath = if(isInstalled) File("ffmpeg") else File(DefaultConfig["ffmpeg.path", File(OS.downloads, "lib\\ffmpeg\\bin\\ffmpeg.exe").toString()])
    var ffprobePath = if(isInstalled) File("ffprobe") else DefaultConfig["ffmpeg.probe.path", File(ffmpegPath.parentFile, "ffprobe.exe")]

    var ffmpegPathString = if(isInstalled) "ffmpeg" else File(DefaultConfig["ffmpeg.path", "lib/ffmpeg/ffmpeg.exe"]).absolutePath
    var ffprobePathString = if(isInstalled) "ffprobe" else DefaultConfig["ffmpeg.probe.path", File(ffmpegPath.parentFile, "ffprobe.exe")].absolutePath

    val ffmpeg get() = if(isInstalled) ffmpegPath else makeSureExists(ffmpegPath) ?: throw RuntimeException("FFmpeg not found! (path: $ffmpegPath), can't use videos, nor webp!")
    val ffprobe get() = if(isInstalled) ffprobePath else makeSureExists(ffprobePath) ?: throw RuntimeException("FFprobe not found! (path: $ffprobePath), can't use videos, nor webp!")

    fun makeSureExists(file: File): File? {
        return if(!file.exists() || file.isDirectory) null
        else file
    }

}