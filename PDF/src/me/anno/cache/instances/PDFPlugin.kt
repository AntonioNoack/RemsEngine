package me.anno.cache.instances

import me.anno.extensions.plugins.Plugin
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.zip.InnerFolderCache
import org.apache.logging.log4j.LogManager

@Suppress("unused")
class PDFPlugin : Plugin() {

    companion object {
        private val LOGGER = LogManager.getLogger(PDFPlugin::class)
    }

    override fun onEnable() {
        super.onEnable()
        // pdf documents
        InnerFolderCache.register("pdf", PDFCache::readAsFolder)
        Thumbs.register("pdf") { srcFile, size, dstFile, callback ->
            srcFile.inputStream { it, exc ->
                if (it != null) {
                    val ref = PDFCache.getDocumentRef(srcFile, it, borrow = true, async = false)
                    if (ref != null) {
                        val image = PDFCache.getImageCachedBySize(ref.doc, size, 0)
                        ref.returnInstance()
                        if (image != null) {
                            Thumbs.saveNUpload(srcFile, false, dstFile, image, callback)
                        } else LOGGER.warn("Couldn't generate image for pdf $srcFile")
                    }
                } else exc?.printStackTrace()
            }
        }
    }

    override fun onDisable() {
        super.onDisable()
        InnerFolderCache.unregister("pdf")
        Thumbs.unregister("pdf")
    }

}