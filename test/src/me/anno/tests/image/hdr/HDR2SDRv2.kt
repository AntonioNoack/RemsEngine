package me.anno.tests.image.hdr

import me.anno.image.ImageCache
import me.anno.io.files.Reference.getReference

fun main() {
    val src = getReference("C:/XAMPP/htdocs/DigitalCampus/images/environment/kloofendal_38d_partly_cloudy_2k.hdr")
    val dst = src.getSiblingWithExtension("jpg")
    ImageCache[src, false]!!.write(dst)
}