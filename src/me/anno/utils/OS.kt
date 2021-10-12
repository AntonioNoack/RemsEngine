package me.anno.utils

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.process.BetterProcessBuilder
import java.lang.management.ManagementFactory
import kotlin.concurrent.thread

object OS {// the os is important for some things, e.g. the allowed file names, and the home directory

    val data = System.getProperty("os.name")
    val isWindows = data.contains("windows", true)
    val isLinux = !isWindows // ^^
    val isMac = false // ^^
    val isIPhoneOS = false // ^^
    val isIPadOS = false // ^^

    // we haven't implemented a mobile version yet (because that needs different controls),
    // and idk about performance
    val isAndroid = isLinux

    val home = getReference(System.getProperty("user.home"))
    val downloads = getReference(home, "Downloads")
    val desktop = getReference(home, "Desktop")
    val documents = getReference(home, "Documents")
    val pictures = getReference(home, "Pictures")
    val videos = getReference(home, "Videos")
    val music = getReference(home, "Music")

    val screenshots = getReference(pictures, "Screenshots")

    fun startProcess(vararg args: String) {
        thread(name = "Process $args") {
            val builder = BetterProcessBuilder(null, args.size, true)
            builder.addAll(args)
            builder.start()
        }
    }

    fun getProcessID(): Int {
        return ManagementFactory.getRuntimeMXBean().name.split("@")[0].toIntOrNull() ?: -1
    }

}