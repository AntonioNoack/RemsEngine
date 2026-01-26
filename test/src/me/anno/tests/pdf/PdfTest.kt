package me.anno.tests.utils

import me.anno.Engine
import me.anno.cache.instances.PDFCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    PDFCache.disableLoggers()
    val file = downloads.getChild("ray differentials.pdf")
    file.inputStream { it, exc ->
        val ref = PDFCache.getDocumentRef(file, it ?: throw exc!!, true, false)!!
        val doc = ref.doc
        PDFCache.getImageCachedBySize(doc, 512, 0)!!
            .write(desktop.getChild("pdfTest.png"))
    }
    Engine.requestShutdown()
}