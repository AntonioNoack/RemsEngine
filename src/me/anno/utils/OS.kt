package me.anno.utils

import me.anno.io.files.Reference.getReference
import me.anno.utils.process.BetterProcessBuilder
import me.anno.utils.types.Ints.toIntOrDefault
import java.lang.management.ManagementFactory
import kotlin.concurrent.thread

/**
 * the os is important for some things, e.g., the allowed file names, and the home directory;
 * this object contains information about which OS/base-engine is used, and where user-related documents are located;
 * */
@Suppress("unused")
object OS {

    private val data: String? = System.getProperty("os.name")

    @JvmField
    var isWindows = data != null && data.contains("windows", true)

    @JvmField
    var isWeb = data == "Linux Web"

    @JvmField
    var isLinux = !isWindows && !isWeb // ^^

    @JvmField
    var isMacOS = false // ^^

    @JvmField
    var isIPhoneOS = false // ^^

    @JvmField
    var isIPadOS = false // ^^

    @JvmField
    var isAndroid = false

    @JvmStatic
    val supportsNetworkUDP get() = !isWeb

    @JvmStatic
    val home by lazy { getReference(System.getProperty("user.home")) }

    @JvmStatic
    val downloads by lazy { home.getChild( "Downloads") }

    @JvmStatic
    val desktop by lazy { home.getChild( "Desktop") }

    @JvmStatic
    val documents by lazy { home.getChild( "Documents") }

    @JvmStatic
    val pictures by lazy { home.getChild( "Pictures") }

    @JvmStatic
    val videos by lazy { home.getChild( "Videos") }

    @JvmStatic
    val music by lazy { home.getChild( "Music") }

    // val res = getReference(BundledRef.prefix) // getChild() is not supported on all platforms, so I'd rather not provide this
    @JvmStatic
    val screenshots by lazy { pictures.getChild("Screenshots") }

    @JvmStatic
    fun startProcess(vararg args: String) {
        thread(name = "Process $args") {
            val builder = BetterProcessBuilder(null, args.size, true)
            builder.add(*args)
            builder.start()
        }
    }

    @JvmStatic
    fun getProcessID(): Int {
        return ManagementFactory.getRuntimeMXBean().name.split("@")[0].toIntOrDefault(-1)
    }

}