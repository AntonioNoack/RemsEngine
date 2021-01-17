package me.anno.video

import java.io.File

open class FFMPEGMeta(file: File?):
    FFMPEGStream(file){

    override fun process(process: Process, arguments: List<String>) {
        getOutput("error", process.errorStream)
        getOutput("input", process.inputStream)
    }

    var stringData = ""

    override fun destroy() {}

}