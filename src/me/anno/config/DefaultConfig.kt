package me.anno.config

import me.anno.Engine.projectName
import me.anno.config.DefaultStyle.baseTheme
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.SaveableArray
import me.anno.io.config.ConfigBasics
import me.anno.io.files.InvalidRef
import me.anno.io.utils.StringMap
import me.anno.ui.base.Font
import me.anno.ui.Style
import me.anno.utils.Clock

object DefaultConfig : StringMap() {

    /**
     * The default style, initialized by config
     * */
    var style: Style = Style("", "")
        get() {
            onSyncAccess() // ensure default style is initialized
            return field
        }

    private var lastProjectName: String? = null

    override fun onSyncAccess() {
        super.onSyncAccess()
        if (projectName != lastProjectName) {
            lastProjectName = projectName
            init()
        }
    }

    private fun init() {

        clear()

        val tick = Clock()

        // in case it wasn't registered yet
        registerCustomClass(StringMap())
        registerCustomClass(SaveableArray())

        tick.stop("registering classes for config")

        var newConfig: StringMap = this
        try {
            newConfig = ConfigBasics.loadConfig("main.config", InvalidRef, this, true)
            putAll(newConfig)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tick.stop("reading base config")

        val stylePath = newConfig["style"]?.toString() ?: "dark"
        style = baseTheme.getStyle(stylePath)

        // not completely true; is loading some classes, too
        tick.stop("reading base style")
    }

    fun defineDefaultFileAssociations() {

        addImportMappings(
            "Image",
            "png", "jpg", "jpeg", "tiff", "webp", "svg", "ico", "psd", "bmp", "jp2", "tga", "dds", "exr"
        )
        addImportMappings("PDF", "pdf")
        addImportMappings("Cubemap-Equ", "hdr")
        addImportMappings(
            "Video",
            "mp4", "m4p", "m4v", "gif", "webm",
            "mpeg", "mp2", "mpg", "mpe", "mpv", "svi", "3gp", "3g2", "roq",
            "nsv", "f4v", "f4p", "f4a", "f4b",
            "avi", "flv", "vob", "wmv", "mkv", "ogg", "ogv", "drc",
            "mov", "qt", "mts", "m2ts", "ts", "rm", "rmvb", "viv", "asf", "amv"
        )
        addImportMappings("Text", "txt")
        addImportMappings("Mesh", "obj", "mtl", "fbx", "dae", "gltf", "glb", "md2", "md5mesh", "vox")
        // not yet supported
        // addImportMappings("Markdown", "md")
        addImportMappings("Audio", "mp3", "wav", "m4a", "ogg", "opus")
        addImportMappings("URL", "url", "lnk", "desktop")
        addImportMappings(
            "Container", "unitypackage",
            "zip", "7z", "tar", "gz", "xz", "rar", "bz2", "xar", "oar",
            "npz" // numpy archive
        )
        addImportMappings("Executable", "exe", "lib", "dll", "pyd", "jar", "desktop")
        addImportMappings("Metadata", "json", "xml")
        addImportMappings("Link", "url", "lnk")
    }

    fun addImportMappings(result: String, vararg extensions: String) {
        for (extension in extensions) {
            // get or set default
            this["import.mapping.$extension", result]
        }
    }

    val defaultFontName get() = this["defaultFont"] as? String ?: "Verdana"
    val defaultFont get() = Font(defaultFontName, 15f, false, false)
    val defaultFont2 get() = Font(defaultFontName, 25f, false, false)
}