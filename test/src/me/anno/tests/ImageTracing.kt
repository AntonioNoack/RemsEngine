package me.anno.tests

import me.anno.image.ImageCPUCache
import me.anno.image.raw.IntImage
import me.anno.tests.gfx.ImageTracing
import me.anno.utils.OS

// test this with real letters
// to do set the test size for meshes to 120 instead of 20-ish
fun main() {
    val image = ImageCPUCache.getImage(OS.documents.getChild("test-text.png"), false)!!
    val pixels = (image as IntImage).data
    val black = -0x1000000
    var i = 0
    val l = pixels.size
    while (i < l) {
        pixels[i] = pixels[i] and black
        i++
    }
    ImageTracing.computeOutline(image.width, image.height, pixels)
}