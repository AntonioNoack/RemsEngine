package me.anno.config

import me.anno.Engine.projectName
import me.anno.config.DefaultStyle.baseTheme
import me.anno.fonts.Font
import me.anno.fonts.FontStats
import me.anno.io.config.ConfigBasics
import me.anno.io.files.InvalidRef
import me.anno.io.utils.StringMap
import me.anno.utils.Clock
import org.apache.logging.log4j.LogManager

object DefaultConfig : StringMap() {

    private val LOGGER = LogManager.getLogger(DefaultConfig::class)

    /**
     * The default style, initialized by config
     * */
    val style by lazy {
        val stylePath = this["style", "dark"]
        baseTheme.getStyle(stylePath)
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

        val tick = Clock(LOGGER)

        // in case it wasn't registered yet
        registerCustomClass(StringMap())

        tick.stop("registering classes for config")

        try {
            putAll(ConfigBasics.loadConfig("main.config", InvalidRef, this, true))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tick.stop("reading base config")
    }

    fun defineDefaultFileAssociations() {
        addImportMappings("Image", "png,jpg,jpeg,tiff,webp,svg,ico,psd,bmp,jp2,tga,dds,exr")
        addImportMappings("PDF", "pdf")
        addImportMappings("Cubemap-Equ", "hdr")
        addImportMappings(
            "Video",
            "mp4,m4p,m4v,gif,webm,mpeg,mp2,mpg,mpe,mpv,svi,3gp,3g2,roq," +
                    "nsv,f4v,f4p,f4a,f4b,avi,flv,vob,wmv,mkv,ogg,ogv,drc,mov,qt,mts,m2ts,ts,rm,rmvb,viv,asf,amv"
        )
        addImportMappings("Text", "txt")
        addImportMappings("Mesh", "obj,mtl,fbx,dae,gltf,glb,ply,md2,md5mesh,vox,blend,blend1")
        // not yet supported
        // addImportMappings("Markdown", "md")
        addImportMappings("Audio", "mp3,wav,m4a,ogg,opus")
        addImportMappings(
            "Container", "unitypackage," +
                    "zip,7z,tar,gz,xz,rar,bz2,xar,oar," +
                    "npz" // numpy archive
        )
        addImportMappings("Executable", "exe,lib,dll,pyd,jar,desktop")
        addImportMappings("Metadata", "json,xml")
        addImportMappings("Link", "url,lnk,desktop")
    }

    fun addImportMapping(result: String, extension: String) {
        // get or set default
        this["import.mapping.$extension", result]
    }

    fun addImportMappings(result: String, extensions: String) {
        addImportMappings(result, extensions.split(','))
    }

    fun addImportMappings(result: String, extensions: Collection<String>) {
        for (extension in extensions) {
            addImportMapping(result, extension)
        }
    }

    val defaultFont
        get() = Font(
            style.getString("text.fontName", "Verdana"),
            style.getSize("text.fontSize", FontStats.getDefaultFontSize()),
            isBold = false,
            isItalic = false
        )
}