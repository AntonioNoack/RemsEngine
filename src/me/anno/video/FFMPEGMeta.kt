package me.anno.video

import java.io.File

open class FFMPEGMeta(file: File?) :
    FFMPEGStream(file, false) {

    override fun process(process: Process, arguments: List<String>) {
        logOutput("error", process.errorStream, true)
        logOutput("input", process.inputStream, false)
    }

    var stringData = ""

    override fun destroy() {}

}