package me.anno.fonts

import me.anno.maths.Packing.pack64
import me.anno.utils.Color.a
import me.anno.utils.Color.rgb
import me.anno.utils.Color.withAlpha
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.arrays.LongArrayList
import me.anno.utils.types.Arrays.indexOf
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.PrintColor.ESC_CHAR

object GlyphStyle {

    private const val QUANT_MASK = (3 shl 24).inv()

    const val BOLD = 1
    const val ITALIC = 2
    const val STRIKETHROUGH = 4
    const val UNDERLINE = 8

    const val STRIKETHROUGH_CHAR = '\u0336'
    const val UNDERLINE_CHAR = '\u035f'

    private const val BOLD_FLAG = BOLD.toLong() shl (24 + 32)
    private const val ITALIC_FLAG = ITALIC.toLong() shl (24 + 32)
    private const val STRIKETHROUGH_FLAG = STRIKETHROUGH.toLong() shl (24 - 2)
    private const val UNDERLINE_FLAG = UNDERLINE.toLong() shl (24 - 2)

    fun encode(
        color: Int, bgColor: Int,
        flags: Int
    ): Long {
        val flagsHigh = flags.and(3).and(3)
        val flagsLow = flags.shr(2)
        val high = quantizeColor(color) or flagsHigh.shl(24)
        val low = quantizeColor(bgColor) or flagsLow.shl(24)
        return pack64(high, low)
    }

    /**
     * remove lowest 2 bits from alpha channel, so we have space for flags
     * */
    private fun quantizeColor(color: Int): Int {
        return color and QUANT_MASK
    }

    /**
     * fill in 2 missing bits from alpha channel
     * */
    private fun dequantizeColor(color: Int): Int {
        var a = (color and QUANT_MASK).a()
        a += a.shr(6) // copy top two bits into lowest places; effectively x = (x>>2)/63*255
        return color.withAlpha(a)
    }

    fun decodeColor(style: Long): Int {
        return dequantizeColor(style.shr(32).toInt())
    }

    fun decodeBgColor(style: Long): Int {
        return dequantizeColor(style.toInt())
    }

    fun isStrikethrough(style: Long): Boolean = style.hasFlag(STRIKETHROUGH_FLAG)
    fun isBold(style: Long): Boolean = style.hasFlag(BOLD_FLAG)
    fun isItalic(style: Long): Boolean = style.hasFlag(ITALIC_FLAG)
    fun isUnderline(style: Long): Boolean = style.hasFlag(UNDERLINE_FLAG)

    fun getColor(color: Int, style: Long): Int {
        val byStyle = decodeColor(style)
        return mixColor(color, byStyle)
    }

    fun getBgColor(color: Int, style: Long): Int {
        val byStyle = decodeBgColor(style)
        return mixColor(color, byStyle)
    }

    private fun mixColor(color: Int, byStyle: Int): Int {
        return if (byStyle.a() == 0) color
        else byStyle.withAlpha(color.a())
    }

    fun extractStyle(text: IntArray, style0: Font): Pair<IntArray, LongArray> {
        val codepoints = IntArrayList(text.size)
        val styles = LongArrayList(text.size)

        var color = 0
        var bgColor = 0
        var flags = style0.isBold.toInt(BOLD) + style0.isItalic.toInt(ITALIC)
        var style = encode(0, 0, flags)

        var i = 0
        while (i < text.size) {
            val codepoint = text[i++]
            if (codepoint == ESC_CHAR.code && i < text.size && text[i] == '['.code) {
                val end = text.indexOf('m'.code, i)
                if (end == -1) break

                val codes = text.joinChars(i + 1, end)
                    .split(';')
                    .mapNotNull { it.toIntOrNull() }

                var ci = 0
                while (ci < codes.size) {
                    when (val code = codes[ci++]) {
                        0 -> {
                            color = 0; bgColor = 0; flags = 0
                        }
                        1 -> flags = flags or BOLD
                        3 -> flags = flags or ITALIC
                        4 -> flags = flags or UNDERLINE
                        9 -> flags = flags or STRIKETHROUGH
                        22 -> flags = flags and BOLD.inv()
                        23 -> flags = flags and ITALIC.inv()
                        24 -> flags = flags and UNDERLINE.inv()
                        29 -> flags = flags and STRIKETHROUGH.inv()

                        in 30..37 -> color = ansiColors[code - 30]
                        in 40..47 -> bgColor = ansiColors[code - 40]
                        38 -> {
                            if (ci < codes.size) {
                                // todo code 5 = 4-bit color via some pre-defined LUT
                                if (codes[ci++] == 2) {
                                    color = rgb(codes[ci++], codes[ci++], codes[ci++])
                                } else ci++ // assume 5, skip one parameter
                            }
                        }
                        48 -> {
                            if (ci < codes.size) {
                                if (codes[ci++] == 2) {
                                    bgColor = rgb(codes[ci++], codes[ci++], codes[ci++])
                                } else ci++ // assume 5, skip one parameter
                            }
                        }
                        39 -> color = 0
                        49 -> bgColor = 0
                    }
                }

                style = encode(color, bgColor, flags)
                i = end + 1
            } else if (codepoints.isNotEmpty() && (codepoint == STRIKETHROUGH_CHAR.code || codepoint == UNDERLINE_CHAR.code)) {
                // handle STRIKETHROUGH_CHAR AND UNDERLINE_CHAR properly -> convert them into styles for the prev charcode!
                val flag = if (codepoint == STRIKETHROUGH_CHAR.code) STRIKETHROUGH_FLAG else UNDERLINE_FLAG
                styles[styles.lastIndex] = styles[styles.lastIndex] or flag
            } else {
                codepoints.add(codepoint)
                styles.add(style)
            }
        }

        return codepoints.toIntArray() to styles.toLongArray()
    }

    // https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit, colors from VSCode
    // (except for bright_white, made it brighter than white)
    private val ansiColors = intArrayOf(
        rgb(0, 0, 0),
        rgb(205, 49, 49),
        rgb(13, 188, 121),
        rgb(229, 229, 16),
        rgb(36, 114, 200),
        rgb(188, 63, 188),
        rgb(17, 168, 205),
        rgb(229, 229, 229),
        rgb(102, 102, 102),
        rgb(241, 76, 76),
        rgb(35, 209, 139),
        rgb(245, 245, 67),
        rgb(59, 142, 234),
        rgb(214, 112, 214),
        rgb(41, 184, 219),
        rgb(242, 242, 242)
    )

}