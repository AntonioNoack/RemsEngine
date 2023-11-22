package me.anno.cache.instances

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.gpu.TextureCache
import me.anno.image.raw.toImage
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCallback
import me.anno.maths.Maths
import me.anno.utils.structures.tuples.Quad
import org.apache.logging.log4j.LogManager
import org.apache.pdfbox.pdfwriter.COSWriter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.roundToInt

object PDFCache : CacheSection("PDFCache") {

    class AtomicCountedDocument(val doc: PDDocument) {

        val counter = AtomicInteger(1)

        fun borrow() {
            counter.incrementAndGet()
        }

        fun returnInstance() {
            if (counter.decrementAndGet() == 0) {
                doc.close()
            }
        }
    }

    fun getDocumentRef(
        src: FileReference,
        input: InputStream,
        borrow: Boolean,
        async: Boolean
    ): AtomicCountedDocument? {
        val data = getEntry(src, timeout, async) {
            val doc = AtomicCountedDocument(
                try {
                    PDDocument.load(input)
                } catch (e: Exception) {
                    LOGGER.error(e.message ?: "Error loading PDF", e)
                    PDDocument()
                }
            )
            object : CacheData<AtomicCountedDocument>(doc) {
                override fun destroy() {
                    value.returnInstance()
                }
            }
        } as? CacheData<*>
        if (!async) input.close()
        val value = data?.value as? AtomicCountedDocument
        if (borrow) value?.borrow()
        return value
    }

    fun readAsFolder(src: FileReference, callback: InnerFolderCallback) {
        src.inputStream { it, exc ->
            if (it != null) {
                val ref = getDocumentRef(src, it, borrow = true, async = false)!!
                val doc = ref.doc
                val folder = InnerFolder(src)
                synchronized(doc) {
                    val numberOfPages = doc.numberOfPages
                    if (numberOfPages > 1) {
                        // not optimal, but finding sth optimal is hard anyway, because
                        // we can read a document only sequentially
                        for (i in 1..numberOfPages) {
                            // don't render them, create a sub-pdfs, which contain a single file each
                            val fileName = "$i.pdf"
                            val page = doc.getPage(i - 1)
                            val doc2 = PDDocument()
                            doc2.addPage(page)
                            val bos = ByteArrayOutputStream(1024)
                            val writer = COSWriter(bos)
                            writer.write(doc2)
                            writer.close()
                            doc2.close()
                            // val image = getImage(doc, 1f, i)
                            // ImageIO.write(image, imageIOFormat, bos)
                            val bytes = bos.toByteArray()
                            folder.createByteChild(fileName, bytes)
                        }
                    } else {
                        // todo create pngs/jpegs of different resolution?
                        // todo create image type from it :)
                        // todo add attribute, that marks it as infinite resolution
                    }
                }
                ref.returnInstance()
                callback(folder, null)
            } else callback(null, exc)
        }
    }

    @Suppress("unused")
    fun getTexture(src: FileReference, doc: PDDocument, quality: Float, pageNumber: Int): Texture2D? {
        val qualityInt = max(1, (quality * 2f).roundToInt())
        val qualityFloat = qualityInt * 0.5f
        val tex = TextureCache.getLateinitTexture(
            Triple(src, qualityInt, pageNumber),
            20_000L,
            false
        ) { callback ->
            thread(name = "PDFCache::getTexture") {
                val image = getImage(doc, qualityFloat, pageNumber)
                GFX.addGPUTask("PDFCache.getTexture()", image.width, image.height) {
                    val tex = Texture2D(src.name, image, true)
                    callback(tex)
                }
            }
        }?.texture as? Texture2D
        return if (tex?.isCreated == true) tex else null
    }

    @Suppress("unused")
    fun getImageCached(doc: PDDocument, dpi: Float, pageNumber: Int): Image? {
        val data = getEntry(Triple(doc, dpi, pageNumber), 10_000, false) {
            CacheData(getImage(doc, dpi, pageNumber))
        } as? CacheData<*>
        return data?.value as? Image
    }

    fun getImageCachedBySize(doc: PDDocument, size: Int, pageNumber: Int): Image? {
        val data = getEntry(Quad(doc, size, pageNumber, ""), 10_000, false) { (doc, size, pageNumber) ->
            CacheData(getImageBySize(doc, size, pageNumber))
        } as? CacheData<*>
        return data?.value as? Image
    }

    @Suppress("unused")
    fun getImageCachedByHeight(doc: PDDocument, height: Int, pageNumber: Int): Image? {
        val data = getEntry(Triple(doc, height, pageNumber), 10_000, false) { (doc, height, pageNumber) ->
            CacheData(getImageByHeight(doc, height, pageNumber))
        } as? CacheData<*>
        return data?.value as? Image
    }

    fun getImageBySize(doc: PDDocument, size: Int, pageNumber: Int): Image {
        val numberOfPages = doc.numberOfPages
        return synchronized(doc) {// has to be synchronous
            val renderer = PDFRenderer(doc)
            val clampedPage = Maths.clamp(pageNumber, 0, numberOfPages - 1)
            val mediaBox = doc.getPage(clampedPage).mediaBox
            val scale = size.toFloat() / max(1f, max(mediaBox.width, mediaBox.height))
            val image = renderer.renderImage(clampedPage, scale, ImageType.RGB)
            image.toImage()
        }
    }

    fun getImageByHeight(doc: PDDocument, height: Int, pageNumber: Int): Image {
        val numberOfPages = doc.numberOfPages
        return synchronized(doc) {// has to be synchronous
            val renderer = PDFRenderer(doc)
            val clampedPage = Maths.clamp(pageNumber, 0, numberOfPages - 1)
            val mediaBox = doc.getPage(clampedPage).mediaBox
            val scale = height.toFloat() / mediaBox.height
            val image = renderer.renderImage(clampedPage, scale, ImageType.RGB)
            image.toImage()
        }
    }

    fun getImage(doc: PDDocument, dpi: Float, pageNumber: Int): Image {
        val numberOfPages = doc.numberOfPages
        val renderer = PDFRenderer(doc)
        return renderer.renderImage(Maths.clamp(pageNumber, 0, numberOfPages - 1), dpi, ImageType.RGB).toImage()
    }

    fun disableLoggers() {
        val lm = LogManager
        lm.disableLogger("GlyphRenderer")
        lm.disableLogger("PDSimpleFont")
        lm.disableLogger("PDICCBased")
        lm.disableLogger("PostScriptTable")
        lm.disableLogger("GlyphSubstitutionTable")
        lm.disableLogger("GouraudShadingContext")
        lm.disableLogger("FontMapperImpl")
        lm.disableLogger("FileSystemFontProvider")
        lm.disableLogger("ScratchFileBuffer")
        lm.disableLogger("FontFileFinder")
        lm.disableLogger("PDFObjectStreamParser")
        lm.disableLogger("TriangleBasedShadingContext")
        lm.disableLogger("Type4ShadingContext")
    }

    private const val timeout = 20_000L
    private val LOGGER = LogManager.getLogger(PDFCache::class)

}