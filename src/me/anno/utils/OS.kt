package me.anno.utils

import java.io.File

object OS {// the os is important for some things, e.g. the allowed file names, and the home directory
    val data = System.getProperty("os.name")
    val isWindows = data.contains("windows", true)
    val isLinux = !isWindows // ^^
    val home = File(System.getProperty("user.home"))
    val downloads = File(home, "Downloads")
    val desktop = File(home, "Desktop")
    val documents = File(home, "Documents")
}