package me.anno.config

import me.anno.config.DefaultStyle.baseTheme
import me.anno.gpu.GFXBase0.projectName
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.SaveableArray
import me.anno.io.config.ConfigBasics
import me.anno.io.unity.UnityReader
import me.anno.io.utils.StringMap
import me.anno.ui.base.Font
import me.anno.ui.style.Style
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager

object DefaultConfig : StringMap() {

    var style: Style = Style("", "")

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

        var newConfig: StringMap = this
        try {
            newConfig = ConfigBasics.loadConfig("main.config", this, true)
            putAll(newConfig)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val stylePath = newConfig["style"]?.toString() ?: "dark"
        style = baseTheme.getStyle(stylePath)

        // not completely true; is loading some classes, too
        tick.stop("reading the config")

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
        addImportMappings("Audio", "mp3", "wav", "m4a", "ogg")
        addImportMappings("URL", "url", "lnk", "desktop")
        addImportMappings("Container", "unitypackage", "zip", "7z", "tar", "gz", "xz", "rar", "bz2", "xar", "oar")
        addImportMappings("Asset", *UnityReader.unityExtensions.toTypedArray())
        addImportMappings("Executable", "exe", "lib", "dll", "pyd", "jar")

    }

    /*fun save() {
        this.wasChanged = false
        baseTheme.values.wasChanged = false
        ConfigBasics.save("main.config", this.toString())
        ConfigBasics.save("style.config", baseTheme.values.toString())
    }*/

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