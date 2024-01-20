package me.anno.video.ffmpeg

import me.anno.io.files.FileReference

open class FFMPEGMeta(file: FileReference?) :
    FFMPEGStream(file, false) {

    override fun process(process: Process, vararg arguments: String) {
        logOutput("error", "FFMPEGMeta", process.errorStream, true)
        logOutput("input", "FFMPEGMeta", process.inputStream, false)
    }

    var stringData = ""

    override fun destroy() {}
}