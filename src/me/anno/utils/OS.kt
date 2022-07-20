package me.anno.utils

import me.anno.io.files.BundledRef
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.process.BetterProcessBuilder
import java.lang.management.ManagementFactory
import kotlin.concurrent.thread

/**
 * the os is important for some things, e.g., the allowed file names, and the home directory;
 * this object contains information about which OS/base-engine is used, and where user-related documents are located;
 * */
object OS {

    val data = System.getProperty("os.name")
    var isWindows = data.contains("windows", true)
    var isLinux = !isWindows // ^^
    var isMac = false // ^^
    var isIPhoneOS = false // ^^
    var isIPadOS = false // ^^

    // we haven't implemented a mobile version yet (because that needs different controls),
    // and idk about performance
    var isAndroid = false

    var isWeb = false

    val supportsNetworkUDP get() = !isWeb

    val home = getReference(System.getProperty("user.home"))
    val downloads = getReference(home, "Downloads")
    val desktop = getReference(home, "Desktop")
    val documents = getReference(home, "Documents")
    val pictures = getReference(home, "Pictures")
    val videos = getReference(home, "Videos")
    val music = getReference(home, "Music")
    val res = getReference(BundledRef.prefix)

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