package me.anno.utils

import me.anno.io.files.BundledRef
import me.anno.io.files.Reference.getReference

/**
 * the os is important for some things, e.g., the allowed file names, and the home directory;
 * this object contains information about which OS/base-engine is used, and where user-related documents are located;
 *
 * todo move most OS checks to OSFeatures-checks, so we can define mixed environments
 * */
@Suppress("unused")
object OS {

    private val osName: String = System.getProperty("os.name") ?: ""

    @JvmField
    var isWindows = osName.contains("windows", true)

    @JvmField
    var isWeb = osName == "Linux Web"

    @JvmField
    var isMacOS = osName.startsWith("Mac OS") // ^^

    @JvmField
    var isLinux = !isWindows && !isWeb && !isMacOS // ^^

    @JvmField
    var isIPhoneOS = false // ^^

    @JvmField
    var isIPadOS = false // ^^

    @JvmField
    var isAndroid = false // set to true by my Android porting runtime

    @JvmStatic
    val home by lazy { getReference(System.getProperty("user.home")) }

    @JvmStatic
    val downloads by lazy { home.getChild("Downloads") }

    @JvmStatic
    val desktop by lazy { home.getChild("Desktop") }

    @JvmStatic
    val documents by lazy { home.getChild("Documents") }

    @JvmStatic
    val pictures by lazy { home.getChild("Pictures") }

    @JvmStatic
    val videos by lazy { home.getChild("Videos") }

    @JvmStatic
    val music by lazy { home.getChild("Music") }

    @JvmStatic
    val res get() = getReference(BundledRef.PREFIX)

    // val res = getReference(BundledRef.prefix) // getChild() is not supported on all platforms, so I'd rather not provide this
    @JvmStatic
    val screenshots by lazy { pictures.getChild("Screenshots") }
}