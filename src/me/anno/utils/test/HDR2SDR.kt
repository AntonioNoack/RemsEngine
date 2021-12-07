package me.anno.utils.test

import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    val src = getReference("C:/XAMPP/htdocs/DigitalCampus/images/environment/kloofendal_38d_partly_cloudy_2k.hdr")
    val dst = src.getSibling("${src.nameWithoutExtension}.jpg")
    ImageCPUCache.getImage(src, false)!!.write(dst)
}