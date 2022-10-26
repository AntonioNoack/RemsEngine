package me.anno.tests.image

import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    val src = getReference("C:/XAMPP/htdocs/DigitalCampus/images/environment/kloofendal_38d_partly_cloudy_2k.hdr")
    val dst = src.getSibling("${src.nameWithoutExtension}.jpg")
    ImageCPUCache[src, false]!!.write(dst)
}