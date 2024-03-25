package me.anno.tests.image.hdr

import me.anno.image.hdr.HDRReader
import me.anno.image.raw.IntImage
import me.anno.io.files.Reference.getReference
import me.anno.utils.Color.rgb
import kotlin.math.sqrt

fun main() {
    val max = 255.5f
    val exposure = 1f
    val ref = getReference("C:/XAMPP/htdocs/uvbaker/env/scythian_tombs_2_4k.hdr")
    val src = ref.inputStreamSync().use(HDRReader::readHDR)
    val srcPixels = src.data
    var j = 0
    val dstPixels = IntArray(src.width * src.height) { i ->
        val r = exposure * srcPixels[j++]
        val g = exposure * srcPixels[j++]
        val b = exposure * srcPixels[j++]
        val rf = r / (1f + r)
        val gf = g / (1f + g)
        val bf = b / (1f + b)
        val ri = (max * sqrt(rf)).toInt()
        val gi = (max * sqrt(gf)).toInt()
        val bi = (max * sqrt(bf)).toInt()
        rgb(ri, gi, bi)
    }
    IntImage(src.width, src.height, dstPixels, false)
        .write(ref.getSiblingWithExtension("png"))
}