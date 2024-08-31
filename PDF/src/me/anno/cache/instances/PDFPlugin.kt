package me.anno.cache.instances

import me.anno.extensions.plugins.Plugin
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.hdb.HDBKey
import me.anno.image.thumbs.Thumbs
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolderCache
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.async.Callback
import org.apache.logging.log4j.LogManager

// todo my downloads folder is still lagging :/, why/how???
@Suppress("unused")
class PDFPlugin : Plugin() {

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(PDFPlugin::class)
        private val pdfThumbnailWorker = ProcessingQueue("PDFThumbnailWorker")
    }

    override fun onEnable() {
        super.onEnable()
        // pdf documents
        PDFCache.disableLoggers()
        InnerFolderCache.registerSignatures("pdf", PDFCache::readAsFolder)
        Thumbs.registerSignatures("pdf", ::generateThumbnail)
    }

    private fun generateThumbnail(srcFile: FileReference, dstFile: HDBKey, size: Int, callback: Callback<ITexture2D>) {
        pdfThumbnailWorker.plusAssign {
            srcFile.inputStream { it, exc ->
                if (it != null) {
                    val ref = PDFCache.getDocumentRef(srcFile, it, borrow = true, async = false)
                    if (ref != null) generateThumbnail(ref, size, srcFile, dstFile, callback)
                } else exc?.printStackTrace()
            }
        }
    }

    private fun generateThumbnail(
        ref: PDFCache.AtomicCountedDocument, size: Int,
        srcFile: FileReference, dstFile: HDBKey, callback: Callback<ITexture2D>
    ) {
        val image = PDFCache.getImageCachedBySize(ref.doc, size, 0)
        ref.returnInstance()
        if (image != null) {
            Thumbs.saveNUpload(srcFile, false, dstFile, image, callback)
        } else LOGGER.warn("Couldn't generate image for pdf {}", srcFile)
    }

    override fun onDisable() {
        super.onDisable()
        InnerFolderCache.unregisterSignatures("pdf")
        Thumbs.unregisterSignatures("pdf")
    }
}