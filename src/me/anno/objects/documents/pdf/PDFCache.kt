package me.anno.objects.documents.pdf

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.instances.TextureCache
import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.utils.Maths
import me.anno.utils.Threads.threadWithName
import org.apache.logging.log4j.LogManager
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.roundToInt

object PDFCache : CacheSection("PDFCache") {

    fun getDocument(src: File, async: Boolean): PDDocument? {
        val data = getEntry(src, timeout, async) {
            val doc = try {
                PDDocument.load(src)
            } catch (e: Exception) {
                LOGGER.error(e.message ?: "Error loading PDF", e)
                PDDocument()
            }
            object : CacheData<PDDocument>(doc) {
                override fun destroy() {
                    value.close()
                }
            }
        } as? CacheData<*>
        return data?.value as? PDDocument
    }

    fun getTexture(src: File, doc: PDDocument, quality: Float, pageNumber: Int): Texture2D? {
        val qualityInt = max(1, (quality * 2f).roundToInt())
        val qualityFloat = qualityInt * 0.5f
        val tex = TextureCache.getLateinitTexture(
            Triple(src, qualityInt, pageNumber),
            PDFDocument.timeout
        ) { callback ->
            threadWithName("PDFCache::getTexture") {
                val image = getImage(doc, qualityFloat, pageNumber)
                GFX.addGPUTask(image.width, image.height) {
                    val tex = Texture2D(image)
                    callback(tex)
                }
            }
        }.texture as? Texture2D
        return if (tex?.isCreated == true) tex else null
    }

    fun getImage(doc: PDDocument, dpi: Float, pageNumber: Int): BufferedImage {
        val numberOfPages = doc.numberOfPages
        val renderer = PDFRenderer(doc)
        // LOGGER.info("${image.getRGB(0, 0)}, should be -1")
        return renderer.renderImage(Maths.clamp(pageNumber, 0, numberOfPages - 1), dpi, ImageType.RGB)
    }

    val timeout = 20_000L
    private val LOGGER = LogManager.getLogger(PDFCache::class)

}