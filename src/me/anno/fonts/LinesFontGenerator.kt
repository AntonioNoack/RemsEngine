package me.anno.fonts

import me.anno.fonts.FontManager.getAvgFontSize
import me.anno.fonts.keys.FontKey
import me.anno.gpu.drawing.DrawTexts.simpleChars
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.texture.FakeWhiteTexture
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.io.base64.Base64
import me.anno.utils.async.Callback
import me.anno.utils.structures.lists.Lists.arrayListOfNulls
import me.anno.utils.types.Booleans.hasFlag
import kotlin.math.min

/**
 * generates a fallback font when other text sources are unavailable using 7-segment-style lines
 * */
class LinesFontGenerator(val key: FontKey) : TextGenerator {

    companion object {
        // - 4 -
        // 0   1
        // - 5 -
        // 2   3
        // - 6 -
        val digits = Base64.decodeBase64("Xwp2eit5fRp/ew")
        val chars = Base64.decodeBase64("P21kbnU1XS8FSj1FPCxsNzskeWVMTFxwa3Y")
        private fun getCharValue(v: Char): Byte {
            return when (v) {
                in '0'..'9' -> digits[v - '0']
                in 'A'..'Z' -> chars[v - 'A']
                in 'a'..'z' -> chars[v - 'a']
                in ",.:;" -> 4
                in "'\"" -> 2
                in "!/\\" -> 5
                ' ' -> 0
                else -> digits[0]
            }
        }

        private fun getSz(size: Int): Int {
            return (size - 5).ushr(1)
        }

        private fun getWidth(sz: Int): Int {
            return sz + 4
        }

        private fun getHeight(sz: Int): Int {
            return sz * 2 + 5
        }
    }

    private val sz = getSz(getAvgFontSize(key.sizeIndex).toInt())
    private val charWidth = getWidth(sz)
    private val charHeight = getHeight(sz)

    private val textures = arrayListOfNulls<IntImage>(128)

    private fun v(image: IntImage, x: Int, y0: Int, dy: Int) {
        for (y in y0 until y0 + dy) image.setRGB(x, y, -1)
    }

    private fun h(image: IntImage, y: Int, x0: Int, dx: Int) {
        for (x in x0 until x0 + dx) image.setRGB(x, y, -1)
    }

    override fun getBaselineY(): Float {
        return charHeight - 1f
    }

    override fun getLineHeight(): Float {
        return charHeight.toFloat()
    }

    private fun generateTexture(v: Char): Image {
        return generateTexture(getCharValue(v))
    }

    private fun generateTexture(b: Byte): IntImage {
        val bi = b.toInt()
        val oldTex = textures[bi]
        if (oldTex != null) {
            return oldTex
        }
        val width = charWidth
        val height = charHeight
        val image = IntImage(width, height, false)
        // draw all lines
        if (bi.hasFlag(1)) v(image, 1, 2, sz)
        if (bi.hasFlag(2)) v(image, sz + 2, 2, sz)
        if (bi.hasFlag(4)) v(image, 1, sz + 3, sz)
        if (bi.hasFlag(8)) v(image, sz + 2, sz + 3, sz)
        if (bi.hasFlag(16)) h(image, 1, 2, sz)
        if (bi.hasFlag(32)) h(image, sz + 2, 2, sz)
        if (bi.hasFlag(64)) h(image, height - 2, 2, sz)
        textures[bi] = image
        return image
    }

    private fun getWrittenLength(text: CharSequence, widthLimit: Int): Int {
        return min(text.length, widthLimit / charWidth)
    }

    override fun calculateSize(text: CharSequence, widthLimit: Int, heightLimit: Int): Int {
        val width = getWrittenLength(text, widthLimit) * charWidth
        val height = charHeight
        return GFXx2D.getSize(width, height)
    }

    override fun generateTexture(
        text: CharSequence,
        widthLimit: Int,
        heightLimit: Int,
        portableImages: Boolean,
        callback: Callback<ITexture2D>,
        textColor: Int,
        backgroundColor: Int
    ) {
        val len = getWrittenLength(text, widthLimit)
        val width = len * charWidth
        val height = charHeight
        if (text.all { getCharValue(it).toInt() == 0 }) {
            return callback.ok(FakeWhiteTexture(width, height, 1))
        } else {
            val image = if (text.length == 1) {
                generateTexture(text[0])
            } else {
                val image = IntImage(width, height, false)
                for (i in 0 until len) {
                    val cv = getCharValue(text[i])
                    if (cv.toInt() != 0) {
                        generateTexture(cv).copyInto(image, i * charWidth, 0)
                    }
                }
                image
            }
            image.createTexture(
                Texture2D(text.toString(), image.width, image.height, 1),
                sync = false, checkRedundancy = false,
                callback
            )
        }
    }

    override fun generateASCIITexture(
        portableImages: Boolean,
        callback: Callback<Texture2DArray>,
        textColor: Int,
        backgroundColor: Int
    ) {
        Texture2DArray("awtAtlas", charWidth, charHeight, simpleChars.size)
            .create(simpleChars.map { generateTexture(it[0]) }, false, callback)
    }
}