package me.anno.tests.gfx.textures

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImageFormat
import me.anno.image.raw.FloatImage
import me.anno.image.raw.IntImage
import me.anno.jvm.HiddenOpenGLContext
import me.anno.tests.image.raw.ByteImageFormatTest.Companion.supportedMask
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertEquals
import me.anno.utils.async.Callback
import org.junit.jupiter.api.Test

class TextureReadPixelTests {

    val w = 17
    val h = 9

    val wx = 5
    val hx = 3

    private fun forAllOffsets(callback: (x: Int, y: Int) -> Unit) {
        for (y in 0 until h - hx step 3) {
            for (x in 0 until w - wx step 4) {
                callback(x, y)
            }
        }
    }

    private fun forAllPixels(w: Int, h: Int, callback: (xi: Int, yi: Int) -> Unit) {
        for (yi in 0 until h) {
            for (xi in 0 until w) {
                callback(xi, yi)
            }
        }
    }

    @Test
    fun testReadFloatPixels() {
        HiddenOpenGLContext.createOpenGL()
        for (numChannels in 1..4) {
            val tex = createFloatTexture(numChannels)
            val readData = FloatArray(wx * hx * numChannels)
            forAllOffsets { ox, oy ->
                tex.readFloatPixels(ox, oy, wx, hx, readData)
                forAllPixels(wx, hx) { xi, yi ->// validate all pixels
                    for (c in 0 until numChannels) {
                        assertEquals(getFloatValue(xi + ox, yi + oy, c), readData[(xi + yi * wx) * numChannels + c])
                    }
                }
            }
        }
    }

    @Test
    fun testReadBytePixels() {
        HiddenOpenGLContext.createOpenGL()
        for (imageFormat in ByteImageFormat.entries) {
            val numChannels = imageFormat.numChannels
            val texture = createByteTexture(imageFormat)
            val readData = ByteArray(wx * hx * numChannels) // this is R/RG/RGB/RGBA
            val readFormat = ByteImageFormat.getRGBAFormat(numChannels)
            val supportedMask = supportedMask(imageFormat.numChannels)
            forAllOffsets { ox, oy ->
                readData.fill(0)
                GFX.check()
                texture.readBytePixels(ox, oy, wx, hx, readData)
                GFX.check()
                forAllPixels(wx, hx) { xi, yi -> // validate all pixels
                    val idx = xi + yi * wx
                    val expectedColor = getIntValue(xi + ox, yi + oy)
                    val readColor = readFormat.fromBytes(readData, idx * numChannels, true)
                    assertEquals(expectedColor and supportedMask, readColor and supportedMask)
                }
            }
            texture.destroy()
        }
    }

    @Test
    fun testReadIntPixelsFromByteTexture() {
        HiddenOpenGLContext.createOpenGL()
        for (imageFormat in ByteImageFormat.entries) {
            if (imageFormat.numChannels != 4) continue // only those are properly supported right now
            val texture = createByteTexture(imageFormat)
            val readData = IntArray(wx * hx)
            val supportedMask = supportedMask(imageFormat.numChannels)
            forAllOffsets { ox, oy ->
                readData.fill(0)
                GFX.check()
                texture.readIntPixels(ox, oy, wx, hx, readData)
                GFX.check()
                forAllPixels(wx, hx) { xi, yi -> // validate all pixels
                    val idx = xi + yi * wx
                    val expectedColor = getIntValue(xi + ox, yi + oy)
                    val readColor = readData[idx]
                    assertEquals(expectedColor and supportedMask, readColor and supportedMask)
                }
            }
            texture.destroy()
        }
    }

    @Test
    fun testReadIntPixelsFromIntTexture() {
        testReadIntPixelsFromIntTexture(true)
        testReadIntPixelsFromIntTexture(false)
    }

    fun testReadIntPixelsFromIntTexture(hasAlphaChannel: Boolean) {
        HiddenOpenGLContext.createOpenGL()
        val texture = createIntTexture(hasAlphaChannel)
        val readData = IntArray(wx * hx)
        val byteData = ByteArray(wx * hx * 3)
        val supportMask = if (hasAlphaChannel) -1 else 0xffffff
        forAllOffsets { ox, oy ->
            readData.fill(0)
            GFX.check()
            if (hasAlphaChannel) {
                texture.readIntPixels(ox, oy, wx, hx, readData)
            } else {
                texture.readBytePixels(ox, oy, wx, hx, byteData)
                for (i in 0 until wx * hx) {
                    readData[i] = ByteImageFormat.RGB.fromBytes(byteData, i * 3, true)
                }
            }
            GFX.check()
            forAllPixels(wx, hx) { xi, yi -> // validate all pixels
                val idx = xi + yi * wx
                val expectedColor = getIntValue(xi + ox, yi + oy) and supportMask
                val readColor = readData[idx] and supportMask
                assertEquals(expectedColor, readColor)
            }
        }
        texture.destroy()
    }

    fun createFloatTexture(numChannels: Int): Texture2D {
        val image = FloatImage(w, h, numChannels)
        image.forEachPixel { x, y ->
            for (c in 0 until numChannels) {
                image.setValue(x, y, c, getFloatValue(x, y, c))
            }
        }
        val texture = Texture2D("textureReadText$numChannels", w, h, 1)
        image.createTexture(texture, true, false, Callback.printError())
        Sleep.waitUntil(true) { texture.isCreated() }
        return texture
    }

    fun createByteTexture(format: ByteImageFormat): Texture2D {
        val image = ByteImage(w, h, format)
        image.forEachPixel { x, y ->
            image.setRGB(x, y, getIntValue(x, y))
        }
        val texture = Texture2D("textureReadText$format", w, h, 1)
        image.createTexture(texture, true, false, Callback.printError())
        Sleep.waitUntil(true) { texture.isCreated() }
        assertEquals(format.numChannels, texture.channels)
        return texture
    }

    fun createIntTexture(hasAlphaChannel: Boolean): Texture2D {
        val image = IntImage(w, h, hasAlphaChannel)
        image.forEachPixel { x, y ->
            image.setRGB(x, y, getIntValue(x, y))
        }
        val texture = Texture2D("textureReadText$hasAlphaChannel", w, h, 1)
        image.createTexture(texture, true, false, Callback.printError())
        Sleep.waitUntil(true) { texture.isCreated() }
        assertEquals(if (hasAlphaChannel) 4 else 3, image.numChannels)
        return texture
    }

    fun getFloatValue(x: Int, y: Int, c: Int): Float {
        return (x + y * w) + c * 0.1f
    }

    fun getIntValue(x: Int, y: Int): Int {
        return (x + y * w) * 0x1020304
    }
}