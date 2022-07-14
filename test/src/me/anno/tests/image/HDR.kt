package me.anno.tests.image

import me.anno.image.hdr.HDRImage
import me.anno.io.files.FileReference
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun main() {
    // test HDR writer using the working HDR reader
    val ref = FileReference.getReference("C:/XAMPP/htdocs/DigitalCampus/images/environment/kloofendal_38d_partly_cloudy_2k.hdr")
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
}