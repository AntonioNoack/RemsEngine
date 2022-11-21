package me.anno.utils

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.process.BetterProcessBuilder
import java.lang.management.ManagementFactory
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

/**
 * the os is important for some things, e.g., the allowed file names, and the home directory;
 * this object contains information about which OS/base-engine is used, and where user-related documents are located;
 * */
object OS {

    val data: String? = System.getProperty("os.name")
    var isWindows = data != null && data.contains("windows", true)
    var isWeb = data == "Linux Web"
    var isLinux = !isWindows && !isWeb // ^^
    var isMacOS = false // ^^
    var isIPhoneOS = false // ^^
    var isIPadOS = false // ^^

    // we haven't implemented a mobile version yet (because that needs different controls),
    // and idk about performance
    var isAndroid = false

    val supportsNetworkUDP get() = !isWeb

    val home by lazy { getReference(System.getProperty("user.home")) }
    val downloads by lazy { getReference(home, "Downloads") }
    val desktop by lazy { getReference(home, "Desktop") }
    val documents by lazy { getReference(home, "Documents") }
    val pictures by lazy { getReference(home, "Pictures") }
    val videos by lazy { getReference(home, "Videos") }
    val music by lazy { getReference(home, "Music") }

    // val res = getReference(BundledRef.prefix) // getChild() is not supported on all platforms, so I'd rather not provide this
    val screenshots by lazy { getReference(pictures, "Screenshots") }

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