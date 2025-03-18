package me.anno.tests.pdf

import me.anno.Engine
import me.anno.cache.instances.PDFCache
import me.anno.engine.OfficialExtensions
import me.anno.io.files.FileReference
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.Floats.f1
import org.apache.pdfbox.pdmodel.PDDocument

fun getMemory(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.totalMemory() - runtime.freeMemory()
}

fun gc() {
    Runtime.getRuntime().gc()
}

fun main() {

    OfficialExtensions.initForTests()

    val m0 = getMemory()
    val src = downloads.getChild("anisotropic metal materials.pdf")
    // val src = downloads.getChild("small.pdf")
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

fun checkGenImage(src: FileReference, doc: PDDocument, size: Int) {
    gc()
    val m2 = getMemory()
    val img2 = PDFCache.getImageBySize(doc, size, 1)
    val m3 = getMemory()
    img2.write(desktop.getChild("${src.nameWithoutExtension}-$size.jpg"))
    val m4 = getMemory()
    val factor1 = (m3 - m2).toFloat() / (3 * size * size)
    val factor2 = (m4 - m3).toFloat() / (3 * size * size)
    println("[$size] ${m2.formatFileSize()} -> ${m3.formatFileSize()} -> ${m4.formatFileSize()} ... [${factor1.f1()}x, ${factor2.f1()}]")
}