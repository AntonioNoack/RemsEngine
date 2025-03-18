package me.anno.tests.pdf

import me.anno.Engine
import me.anno.cache.instances.PDFCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.downloads
import me.anno.utils.files.Files.formatFileSize
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File

fun extractPage(inputPdf: String, outputPdf: String, pageNumber: Int) {
    PDDocument.load(File(inputPdf)).use { document ->
        val newDoc = PDDocument()
        newDoc.addPage(document.getPage(pageNumber))
        newDoc.save(outputPdf)
        newDoc.close()
    }
}

fun main() {

    // having a smaller PDF file doesn't help...
    OfficialExtensions.initForTests()

    val m0 = getMemory()
    val src0 = downloads.getChild("anisotropic metal materials.pdf")
    val src = src0.getSiblingWithExtension("1.pdf")
    // extractPage(src0.absolutePath, src.absolutePath, 1)

    val m1 = getMemory()
    val bytes = src.readBytesSync()
    val m2p1 = getMemory()
    val doc = PDFCache.getDocumentRef(src, bytes.inputStream(), true, false)!!
    val m2p2 = getMemory()
    println(
        "${m0.formatFileSize()},${src.length().formatFileSize()} -> " +
                "${m1.formatFileSize()} -> " +
                "${m2p1.formatFileSize()} -> " +
                "${m2p2.formatFileSize()} ... "
    )
    checkGenImage(src, doc.doc, 16)
    checkGenImage(src, doc.doc, 16)
    checkGenImage(src, doc.doc, 256)
    checkGenImage(src, doc.doc, 1024)
    checkGenImage(src, doc.doc, 1024 * 8)
    Engine.requestShutdown()
}
