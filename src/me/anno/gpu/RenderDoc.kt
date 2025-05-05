package me.anno.gpu

import me.anno.Build
import me.anno.config.DefaultConfig
import me.anno.io.files.Reference.getReference
import me.anno.utils.OSFeatures
import org.apache.logging.log4j.LogManager

/**
 * RenderDoc is a graphics debugging tool.
 * Rem's Engine supports starting with RenderDoc attached,
 * we just have to initialize RenderDoc before OpenGL.
 * */
object RenderDoc {

    private val LOGGER = LogManager.getLogger(RenderDoc::class)

    /** must be executed before OpenGL-init;
     * must be disabled for Nvidia Nsight */
    @JvmStatic
    private var disableRenderDoc = false

    @JvmStatic
    fun disableRenderDoc() {
        disableRenderDoc = true
    }

    @JvmStatic
    fun loadRenderDoc() {
        val enabled = DefaultConfig["debug.renderdoc.enabled", Build.isDebug]
        if (enabled && !disableRenderDoc) {
            forceLoadRenderDoc()
        }
    }

    @JvmStatic
    fun forceLoadRenderDoc(renderDocPath: String? = null) {
        if (OSFeatures.mayLoadRenderDocExplicitly) return // not supported
        val path = renderDocPath ?: DefaultConfig["debug.renderdoc.path", "C:/Program Files/RenderDoc/renderdoc.dll"]
        try {
            // if renderdoc is installed on linux, or given in the path, we could use it as well with loadLibrary()
            // at least this is the default location for RenderDoc
            if (getReference(path).exists) {
                LOGGER.info("Loading RenderDoc")
                System.load(path)
                WindowManagement.usesRenderDoc = true
            } else LOGGER.warn("Did not find RenderDoc, searched '$path'")
        } catch (e: Exception) {
            LOGGER.warn("Could not initialize RenderDoc")
            e.printStackTrace()
        }
    }
}