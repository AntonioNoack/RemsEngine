package me.anno.tests.utils

import me.anno.Engine
import me.anno.io.files.FileWatch
import me.anno.utils.OS

fun main() {
    val tmpDir = OS.desktop.getChild("tmp")
    tmpDir.mkdirs()
    val tmpFile = tmpDir.getChild("x.txt")
    FileWatch.addWatchDog(tmpFile)
    Thread.sleep(100)
    tmpFile.writeText("") // must be registered
    Thread.sleep(100)
    tmpFile.writeText("hey") // must be registered
    Thread.sleep(100)
    tmpFile.writeBytes(byteArrayOf(1, 2, 3))
    Thread.sleep(100)
    FileWatch.removeWatchDog(tmpFile)
    Thread.sleep(100)
    tmpFile.delete() // should not be registered
    Thread.sleep(100)
    Engine.requestShutdown()
}