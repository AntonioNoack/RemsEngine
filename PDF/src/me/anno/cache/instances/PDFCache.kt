package me.anno.cache.instances

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.image.Image
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCallback
import me.anno.jvm.images.BIImage.toImage
import me.anno.maths.Maths
import me.anno.utils.types.Floats.roundToIntOr
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

object PDFCache : CacheSection("PDFCache") {

    // because this library isn't thread safe in the slightest :(
    private object Synchronizer

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
        val data = getEntry(src, TIMEOUT, async) {
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
        }
        if (!async) input.close()
        val value = data?.value
        if (borrow) value?.borrow()
        return value
    }

    fun readAsFolder(src: FileReference, callback: InnerFolderCallback) {
        println("reading $src as PDF folder")
        src.inputStream { it, exc ->
            if (it != null) {
                val ref = getDocumentRef(src, it, borrow = true, async = false)!!
                val doc = ref.doc
                val folder = InnerFolder(src)
                synchronized(Synchronizer) {
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
                        for (pow in 6..12) {
                            val size = 1 shl pow
                            folder.createLazyImageChild("${size}px.png", lazy {
                                getImageCachedBySize(doc, size, 1)!!
                            }, null)
                        }
                    }
                }
                ref.returnInstance()
                callback.ok(folder)
            } else callback.err(exc)
        }
    }

    @Suppress("unused")
    fun getTexture(src: FileReference, doc: PDDocument, quality: Float, pageNumber: Int): Texture2D? {
        val qualityInt = max(1, (quality * 2f).roundToIntOr())
        val qualityFloat = qualityInt * 0.5f
        val tex = TextureCache.getLateinitTexture(
            Triple(src, qualityInt, pageNumber),
            20_000L, false
        ) { callback ->
            thread(name = "PDFCache::getTexture") {
                val image = getImage(doc, qualityFloat, pageNumber)
                addGPUTask("PDFCache.getTexture()", image.width, image.height) {
                    callback.ok(Texture2D(src.name, image, true))
                }
            }
        }?.value as? Texture2D
        return if (tex?.wasCreated == true) tex else null
    }

    data class Key(val doc: PDDocument, val dpi: Float, val pageNumber: Int)
    data class SizeKey(val doc: PDDocument, val size: Int, val pageNumber: Int)
    data class HeightKey(val doc: PDDocument, val height: Int, val pageNumber: Int)

    @Suppress("unused")
    fun getImageCached(doc: PDDocument, dpi: Float, pageNumber: Int): Image? {
        return getEntry(Key(doc, dpi, pageNumber), 10_000, false) {
            CacheData(getImage(doc, dpi, pageNumber))
        }?.value
    }

    fun getImageCachedBySize(doc: PDDocument, size: Int, pageNumber: Int): Image? {
        return getEntry(SizeKey(doc, size, pageNumber), 10_000, false) { (doc, size, pageNumber) ->
            CacheData(getImageBySize(doc, size, pageNumber))
        }?.value
    }

    @Suppress("unused")
    fun getImageCachedByHeight(doc: PDDocument, height: Int, pageNumber: Int): Image? {
        return getEntry(HeightKey(doc, height, pageNumber), 10_000, false) { (doc, height, pageNumber) ->
            CacheData(getImageByHeight(doc, height, pageNumber))
        }?.value
    }

    fun getImageBySize(doc: PDDocument, size: Int, pageNumber: Int): Image {
        val numberOfPages = doc.numberOfPages
        return synchronized(Synchronizer) {// has to be synchronous
            val renderer = PDFRenderer(doc)
            val clampedPage = Maths.clamp(pageNumber, 0, numberOfPages - 1)
            val mediaBox = doc.getPage(clampedPage).mediaBox
            val scale = size.toFloat() / max(1f, max(mediaBox.width, mediaBox.height))
            renderer.renderImage(clampedPage, scale, ImageType.RGB)
        }.toImage()
    }

    fun getImageByHeight(doc: PDDocument, height: Int, pageNumber: Int): Image {
        val numberOfPages = doc.numberOfPages
        return synchronized(Synchronizer) {// has to be synchronous
            val renderer = PDFRenderer(doc)
            val clampedPage = Maths.clamp(pageNumber, 0, numberOfPages - 1)
            val mediaBox = doc.getPage(clampedPage).mediaBox
            val scale = height.toFloat() / mediaBox.height
            renderer.renderImage(clampedPage, scale, ImageType.RGB)
        }.toImage()
    }

    fun getImage(doc: PDDocument, dpi: Float, pageNumber: Int): Image {
        return synchronized(Synchronizer) {
            val numberOfPages = doc.numberOfPages
            val renderer = PDFRenderer(doc)
            renderer.renderImage(Maths.clamp(pageNumber, 0, numberOfPages - 1), dpi, ImageType.RGB)
        }.toImage()
    }

    fun disableLoggers() {
        LogManager.disableLoggers(
            "GlyphRenderer,PDSimpleFont,PDICCBased,PostScriptTable,GlyphSubstitutionTable," +
                    "GouraudShadingContext,FontMapperImpl,FileSystemFontProvider,ScratchFileBuffer,FontFileFinder," +
                    "PDFObjectStreamParser,TriangleBasedShadingContext,Type4ShadingContext"
        )
    }

    private const val TIMEOUT = 20_000L
    private val LOGGER = LogManager.getLogger(PDFCache::class)
}