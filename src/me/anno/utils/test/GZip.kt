package me.anno.utils.test

import me.anno.utils.OS
import java.util.zip.GZIPInputStream

fun main() {

    val src = OS.downloads.getChild("PolygonSciFiCity_Unity_Project_2017_4.unitypackage")
    val input = GZIPInputStream(src.inputStream())

    val dst = OS.downloads.getChild("test.tar")
    dst.writeBytes(input.readBytes())

}