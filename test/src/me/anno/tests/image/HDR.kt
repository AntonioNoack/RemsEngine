package me.anno.tests.image

import me.anno.image.hdr.HDRReader
import me.anno.image.raw.IntImage
import me.anno.io.files.Reference.getReference
import me.anno.utils.Color.rgb
import kotlin.math.sqrt

/*fun main() {
    // test HDR writer using the working HDR reader
    val ref = getReference("C:/XAMPP/htdocs/DigitalCampus/images/environment/kloofendal_38d_partly_cloudy_2k.hdr")
    val correctInput = HDRImage(ref)
    val createdStream = ByteArrayOutputStream(correctInput.width * correctInput.height * 4)
    HDRImage.writeHDR(correctInput.width, correctInput.height, correctInput.pixels, createdStream)
    val createdBytes = createdStream.toByteArray()
    val testedInputStream = ByteArrayInputStream(createdBytes)
    val testedInput = HDRImage(testedInputStream)
    val correctPixels = correctInput.pixels
    val testedPixels = testedInput.pixels
    if (correctPixels.size != testedPixels.size) throw RuntimeException("Size doesn't match!")
    for (i in correctPixels.indices) {
        if (correctPixels[i] != testedPixels[i]) {
            throw RuntimeException("Pixels don't match! " + correctPixels[i] + " vs " + testedPixels[i] + " at index " + i)
        }
    }
    println("Test passed")
}*/

fun main() {

    val ref = getReference("C:/XAMPP/htdocs/uvbaker/env/scythian_tombs_2_4k.hdr")
    val exposure = 1f

    val src = ref.inputStreamSync().use(HDRReader::read)
    val dst = IntImage(src.width, src.height, false)
    val pixels = src.data
    var j = 0
    val max = 255.5f
    for (i in 0 until src.width * src.height) {
        val r = exposure * pixels[j++]
        val g = exposure * pixels[j++]
        val b = exposure * pixels[j++]
        val rf = r / (1f + r)
        val gf = g / (1f + g)
        val bf = b / (1f + b)
        val ri = (max * sqrt(rf)).toInt()
        val gi = (max * sqrt(gf)).toInt()
        val bi = (max * sqrt(bf)).toInt()
        dst.data[i] = rgb(ri, gi, bi)
    }
    dst.write(ref.getSiblingWithExtension("png"))
}