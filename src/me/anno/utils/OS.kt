package me.anno.utils

import java.io.File
import kotlin.concurrent.thread

object OS {// the os is important for some things, e.g. the allowed file names, and the home directory

    val data = System.getProperty("os.name")
    val isWindows = data.contains("windows", true)
    val isLinux = !isWindows // ^^

    // we haven't implemented a mobile version yet (because that needs different controls),
    // and idk about performance
    val isAndroid = isLinux

    val home = File(System.getProperty("user.home"))
    val downloads = File(home, "Downloads")
    val desktop = File(home, "Desktop")
    val documents = File(home, "Documents")
    val pictures = File(home, "Pictures")

    fun startProcess(vararg args: String) {
        thread {
            ProcessBuilder(args.toList()).start()
        }
    }

}