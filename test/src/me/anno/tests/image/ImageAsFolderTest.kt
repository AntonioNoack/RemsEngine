package me.anno.tests.image

import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ImageAsFolderTest {

    val base = IntImage(1, 4, intArrayOf(0xff0000, 0x00ff00, 0x0000ff, white), true)

    fun expectImage(colors: IntArray, file: FileReference) {
        val loadedImage = ImageCache[file, false]
        assertNotNull(loadedImage)
        assertEquals(colors, loadedImage!!.asIntImage().data)
    }

    @Test
    fun testImages() {
        val ref = base.ref
        expectImage(base.data, ref)
        expectImage(intArrayOf(white, black, black, white), ref.getChild("r.png"))
        expectImage(intArrayOf(black, white, black, white), ref.getChild("g.png"))
        expectImage(intArrayOf(black, black, white, white), ref.getChild("b.png"))
        expectImage(intArrayOf(black, black, black, white), ref.getChild("a.png"))
        expectImage(intArrayOf(0x0000ff, 0x00ff00, 0xff0000, white), ref.getChild("bgra.png"))
        expectImage(intArrayOf(black, white, white, black), ref.getChild("1-r.png"))
        expectImage(intArrayOf(white, black, white, black), ref.getChild("1-g.png"))
        expectImage(intArrayOf(white, white, black, black), ref.getChild("1-b.png"))
        expectImage(intArrayOf(white, white, white, black), ref.getChild("1-a.png"))
        expectImage(intArrayOf(0xffffff, 0xffffff, 0xffffff, white), ref.getChild("111a.png"))
        expectImage(intArrayOf(0x000000, 0x000000, 0x000000, black), ref.getChild("000a.png"))
        expectImage(intArrayOf(0xff0000 or black, 0x00ff00 or black, 0x0000ff or black, white), ref.getChild("rgb.png"))
    }
}