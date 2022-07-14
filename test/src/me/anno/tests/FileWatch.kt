package me.anno.tests

import me.anno.Engine
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.utils.OS

fun main() {
    val tmpDir = FileReference.getReference(OS.desktop, "tmp")
    tmpDir.mkdirs()
    val tmpFile = FileReference.getReference(tmpDir, "x.txt")
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