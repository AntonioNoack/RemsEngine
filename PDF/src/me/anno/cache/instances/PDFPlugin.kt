package me.anno.cache.instances

import me.anno.extensions.plugins.Plugin
import me.anno.image.thumbs.Thumbs
import me.anno.io.files.inner.InnerFolderCache
import org.apache.logging.log4j.LogManager

@Suppress("unused")
class PDFPlugin : Plugin() {

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(PDFPlugin::class)
    }

    override fun onEnable() {
        super.onEnable()
        // pdf documents
        PDFCache.disableLoggers()
        InnerFolderCache.registerSignatures("pdf", PDFCache::readAsFolder)
        Thumbs.registerSignatures("pdf") { srcFile, dstFile, size, callback ->
            srcFile.inputStream { it, exc ->
                if (it != null) {
                    val ref = PDFCache.getDocumentRef(srcFile, it, borrow = true, async = false)
                    if (ref != null) {
                        val image = PDFCache.getImageCachedBySize(ref.doc, size, 0)
                        ref.returnInstance()
                        if (image != null) {
                            Thumbs.saveNUpload(srcFile, false, dstFile, image, callback)
                        } else LOGGER.warn("Couldn't generate image for pdf {}", srcFile)
                    }
                } else exc?.printStackTrace()
            }
        }
    }

    override fun onDisable() {
        super.onDisable()
        InnerFolderCache.unregisterSignatures("pdf")
        Thumbs.unregisterSignatures("pdf")
    }

}