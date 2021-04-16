package me.anno.utils

import me.anno.io.FileReference
import java.io.File
import kotlin.concurrent.thread

object OS {// the os is important for some things, e.g. the allowed file names, and the home directory

    val data = System.getProperty("os.name")
    val isWindows = data.contains("windows", true)
    val isLinux = !isWindows // ^^
    val isMac = false // ^^

    // we haven't implemented a mobile version yet (because that needs different controls),
    // and idk about performance
    val isAndroid = isLinux

    val home = FileReference(System.getProperty("user.home"))
    val downloads = FileReference(home, "Downloads")
    val desktop = FileReference(home, "Desktop")
    val documents = FileReference(home, "Documents")
    val pictures = FileReference(home, "Pictures")
    val videos = FileReference(home, "Videos")
    val music = FileReference(home, "Music")

    fun startProcess(vararg args: String) {
        thread {
            ProcessBuilder(args.toList()).start()
        }
    }

}