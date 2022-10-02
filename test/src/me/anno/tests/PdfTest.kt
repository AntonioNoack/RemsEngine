package me.anno.tests

import me.anno.cache.instances.PDFCache
import me.anno.image.raw.write
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

fun main() {
    val file = downloads.getChild("22_08_10_poster_share.pdf")
    val ref = PDFCache.getDocumentRef(file, file.inputStreamSync(), true, false)!!
    val doc = ref.doc
    println(doc)
    val img = PDFCache.getImageCachedBySize(doc, 512, 0)
    println(img)
    img!!.write(desktop.getChild("pdfTest.png"))
}