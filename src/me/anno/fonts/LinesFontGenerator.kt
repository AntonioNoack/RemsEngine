package me.anno.fonts

import me.anno.image.raw.IntImage
import me.anno.io.base64.Base64
import me.anno.utils.types.Booleans.hasFlag

/**
 * generates a fallback font when other text sources are unavailable using 7-segment-style lines
 * */
object LinesFontGenerator : FontImpl<Unit>() {

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

    private fun sz(font: Font) = getSz(font.sizeInt)
    private fun charWidth(font: Font) = getWidth(sz(font))
    private fun charHeight(font: Font) = getHeight(sz(font))

    private fun v(image: IntImage, x: Int, y0: Int, dy: Int) {
        for (y in y0 until y0 + dy) image.setRGB(x, y, -1)
    }

    private fun h(image: IntImage, y: Int, x0: Int, dx: Int) {
        for (x in x0 until x0 + dx) image.setRGB(x, y, -1)
    }

    override fun getBaselineY(font: Font): Float {
        return charHeight(font) - 1f
    }

    override fun getLineHeight(font: Font): Float {
        return charHeight(font).toFloat()
    }

    override fun getTextLength(font: Font, codepoint: Int): Int {
        return charWidth(font)
    }

    override fun getTextLength(font: Font, codepointA: Int, codepointB: Int): Int {
        return charWidth(font) * 2 + 1
    }

    override fun drawGlyph(
        image: IntImage,
        x0: Int, x1: Int, y0: Int, y1: Int, strictBounds: Boolean,
        font: Font, fallbackFonts: Unit, fontIndex: Int,
        codepoint: Int, textColor: Int, backgroundColor: Int,
        portableImages: Boolean
    ) {
        if (codepoint > 0xffff) return
        val bi = getCharValue(codepoint.toChar()).toInt()
        val height = charHeight(font)
        val sz = sz(font)
        // draw all lines
        if (bi.hasFlag(1)) v(image, x0 + 1, y0 + 2, sz)
        if (bi.hasFlag(2)) v(image, x0 + sz + 2, y0 + 2, sz)
        if (bi.hasFlag(4)) v(image, x0 + 1, y0 + sz + 3, sz)
        if (bi.hasFlag(8)) v(image, x0 + sz + 2, y0 + sz + 3, sz)
        if (bi.hasFlag(16)) h(image, y0 + 1, x0 + 2, sz)
        if (bi.hasFlag(32)) h(image, y0 + sz + 2, x0 + 2, sz)
        if (bi.hasFlag(64)) h(image, y0 + height - 2, x0 + 2, sz)
    }

    override fun getFallbackFonts(font: Font): Unit = Unit
    override fun getSupportLevel(fonts: Unit, codepoint: Int, lastSupportLevel: Int): Int = 0
}