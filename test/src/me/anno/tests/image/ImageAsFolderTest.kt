package me.anno.tests.image

import me.anno.Engine
import me.anno.image.ImageCache
import me.anno.image.ImageReadable
import me.anno.image.raw.AlphaMaskImage
import me.anno.image.raw.BGRAImage
import me.anno.image.raw.ComponentImage
import me.anno.image.raw.GrayscaleImage
import me.anno.image.raw.IntImage
import me.anno.image.raw.OpaqueImage
import me.anno.io.files.FileReference
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertIs
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class ImageAsFolderTest {

    private val red = 0xff0000
    private val green = 0x00ff00
    private val blue = 0x0000ff

    val base = IntImage(1, 4, intArrayOf(red, green, blue, white), true)

    fun expectImage(colors: IntArray, file: FileReference, expectedClass: KClass<*>) {
        val loadedImage = ImageCache[file, false]
        assertNotNull(loadedImage)
        assertIs(expectedClass, loadedImage)
        assertEquals(colors, loadedImage!!.asIntImage().data)
        assertEquals(base.width, loadedImage.width)
        assertEquals(base.height, loadedImage.height)
    }

    @Test
    fun testImages() {
        HiddenOpenGLContext.createOpenGL()
        val ref = base.ref
        expectImage(base.data, ref, IntImage::class)
        assertIs(ImageReadable::class, ref.getChild("r.png"))
        expectImage(intArrayOf(white, black, black, white), ref.getChild("r.png"), ComponentImage::class)
        expectImage(intArrayOf(black, white, black, white), ref.getChild("g.png"), ComponentImage::class)
        expectImage(intArrayOf(black, black, white, white), ref.getChild("b.png"), ComponentImage::class)
        expectImage(intArrayOf(black, black, black, white), ref.getChild("a.png"), ComponentImage::class)
        expectImage(intArrayOf(blue, green, red, white), ref.getChild("bgra.png"), BGRAImage::class)
        expectImage(intArrayOf(black, white, white, black), ref.getChild("1-r.png"), ComponentImage::class)
        expectImage(intArrayOf(white, black, white, black), ref.getChild("1-g.png"), ComponentImage::class)
        expectImage(intArrayOf(white, white, black, black), ref.getChild("1-b.png"), ComponentImage::class)
        expectImage(intArrayOf(white, white, white, black), ref.getChild("1-a.png"), ComponentImage::class)
        expectImage(intArrayOf(0xffffff, 0xffffff, 0xffffff, white), ref.getChild("111a.png"), AlphaMaskImage::class)
        expectImage(intArrayOf(0x000000, 0x000000, 0x000000, black), ref.getChild("000a.png"), AlphaMaskImage::class)
        expectImage(
            intArrayOf(red or black, green or black, blue or black, white),
            ref.getChild("rgb.png"), OpaqueImage::class
        )
        expectImage(
            intArrayOf(0x363636 or black, 0xb6b6b6 or black, 0x121212 or black, white),
            ref.getChild("grayscale.png"), GrayscaleImage::class
        )
        Engine.requestShutdown()
    }
}