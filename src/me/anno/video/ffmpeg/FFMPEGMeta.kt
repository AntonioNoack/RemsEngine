package me.anno.video.ffmpeg

import me.anno.io.files.FileReference

open class FFMPEGMeta(file: FileReference?) :
    FFMPEGStream(file, false) {

    override fun process(process: Process, arguments: List<String>) {
        logOutput("error", process.errorStream, true)
        logOutput("input", process.inputStream, false)
    }

    var stringData = ""

    override fun destroy() {}

}