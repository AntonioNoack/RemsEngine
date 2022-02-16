package me.anno.ui.editor.code

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig.style
import me.anno.config.DefaultStyle
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.xml.XMLReader.skipN
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mixARGB
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.Font
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.scrolling.LongScrollable
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.hex4
import me.anno.utils.input.Input.readNBytes2
import kotlin.math.ceil
import kotlin.math.log2

// todo idea: move inFocus into WindowStack, because it really depends only on the stack

// todo hex editor like HxD, but better comparison mode
// todo maybe next to each other?, and then synchronize scroll lists? lock/unlock them? :)
// todo support deeper scrolling with scrollbar? mmh...


// todo selecting text
// todo comparing stuff
// todo edit bytes
// todo copy-pasting
// todo data inspector (base 10, fp16, fp32, fp64, ... le/be)

class HexEditor(style: Style) : Panel(style), LongScrollable {

    var file: FileReference = InvalidRef

    val padding = Padding(4)
    var font = Font("Courier New", 16)

    var bytesPerLine = 16
    var spacing = 0.5f

    val charWidth get() = font.sampleWidth
    val lineHeight get() = font.sampleHeight

    val cursor0 = 0L
    val cursor1 = 0L

    var showAddress = true
    var addressDigits = 0

    override val sizeX get() = minW.toLong()
    override var sizeY = 0L

    override fun setExtraScrolling(vx: Long, vy: Long) {
        extraScrolling = vy
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        val spacing = (spacing * charWidth).toInt()
        val baseW = bytesPerLine * (charWidth * 2 + spacing) - spacing
        val fileLength = file.length()
        val lineCount = ceilDiv(fileLength, bytesPerLine.toLong())
        sizeY = lineHeight * lineCount + padding.height
        minW = baseW + padding.width
        if (showText) {
            minW += bytesPerLine * charWidth
        }
        minH = clamp(sizeY, 0L, 100_000L).toInt()
        if (showAddress) {
            addressDigits = max(1, ceil(log2(fileLength.toDouble()) * 0.25).toInt())
            minW += spacing + charWidth * addressDigits
        } else addressDigits = 0
        this.w = minW
        this.h = minH
    }

    override val canDrawOverBorders: Boolean = true

    var textColor = -1 and (0xa0.shl(24) or 0xffffff)
    var midLineColor = mixARGB(textColor, backgroundColor, 0.5f)
    var lineEveryN = 4
    var showText = true

    var extraScrolling = 0L

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        // calculate line number
        val bx = x + padding.left
        val by = y + padding.top - extraScrolling
        val l0 = max(0, (y0 - by) / lineHeight)
        val lineCount = ceilDiv(file.length(), bytesPerLine.toLong())
        val l1 = min(ceilDiv(y1 - by, lineHeight.toLong()), lineCount)
        val bc = backgroundColor
        val tc = textColor
        val spacing = (spacing * charWidth).toInt()
        val addressDigits = addressDigits
        val addressDx = spacing + charWidth * addressDigits
        for (lineNumber in l0 until l1) {
            val address = lineNumber * bytesPerLine
            // draw address
            for (digitIndex in 0 until addressDigits) {
                val digit = address.shr((addressDigits - 1 - digitIndex) * 4)
                // group these in pairs as well
                val pairOffset = if ((digitIndex + addressDigits).and(1) > 0) -1 else +1
                drawChar(digitIndex, lineNumber, pairOffset, hex4(digit), tc, bc)
            }
        }
        lateinit var buffer: ByteArray
        var lastBufferIndex = -1L
        loop@ for (lineNumber in l0 until l1) {
            for (lineIndex in 0 until bytesPerLine) {
                val byteIndex = lineNumber * bytesPerLine + lineIndex
                val bufferIndex = byteIndex / sectionSize
                if (bufferIndex != lastBufferIndex) {
                    buffer = Companion.get(file, bufferIndex, false) ?: break@loop
                    lastBufferIndex = bufferIndex
                }
                val localIndex = (byteIndex and (sectionSize - 1L)).toInt()
                if (localIndex >= buffer.size) break@loop
                val value = buffer[localIndex].toInt() and 0xff
                val ox = lineIndex * spacing + addressDx
                // draw hex
                drawChar(lineIndex * 2, lineNumber, ox + 1, hex4(value.shr(4)), tc, bc)
                drawChar(lineIndex * 2 + 1, lineNumber, ox - 1, hex4(value), tc, bc)
                if (showText) {
                    // draw byte as char
                    val ox2 = bytesPerLine * spacing + addressDx
                    drawChar(bytesPerLine * 2 + lineIndex, lineNumber, ox2, displayedBytes[value], tc, bc)
                }
            }
        }
        // separation lines
        val yl0 = max(y0.toLong(), by)
        val yl1 = (min(y1.toLong(), by + lineCount * lineHeight) - yl0).toInt()
        val yl0i = yl0.toInt()
        val lineColor = mixARGB(backgroundColor, midLineColor, midLineColor.a() / 255f)
        for (i in 0 until bytesPerLine step lineEveryN) {
            val x2 = bx + addressDx + i * (spacing + 2 * charWidth) - (spacing + 1) / 2
            val lineColor2 = if (i > 0) mixARGB(backgroundColor, lineColor, 0.3f) else lineColor
            drawRect(x2, yl0i, 1, yl1, lineColor2)
        }
        val x2 = bx + addressDx + bytesPerLine * (spacing + 2 * charWidth) - (spacing + 1) / 2
        drawRect(x2, yl0i, 1, yl1, lineColor)
    }

    fun drawChar(
        xi: Int, yi: Long, ox: Int, char: Char,
        textColor: Int, backgroundColor: Int
    ) {
        val code = char.code
        if (code in 0 until cacheSize) {
            var tw = textSizes[code]
            var key = textCacheKeyCache[code]
            if (tw <= 0) {
                val text = char.toString()
                tw = getTextSizeX(font, text, -1, -1)
                textSizes[code] = tw
                key = TextCacheKey(text, font)
                textCacheKeyCache[code] = key
            }
            key!!
            val background = backgroundColor
            val x = this.x + padding.left + xi * charWidth + ox
            val y = this.y + padding.top + yi * lineHeight - extraScrolling
            if (backgroundColor != this.backgroundColor) {
                drawRect(x, y.toInt(), charWidth, lineHeight, background or DefaultStyle.black)
            }
            drawText(x - (charWidth - tw) / 2, y.toInt(), font, key, textColor, background and 0xffffff)
        } else drawChar2(xi, yi, ox, char, textColor, backgroundColor)
    }

    private fun drawChar2(
        xi: Int, yi: Long, ox: Int, char: Char,
        textColor: Int, backgroundColor: Int
    ) {
        val text = char.toString()
        val key = TextCacheKey(text, font)
        val background = backgroundColor
        val x = this.x + padding.left + xi * charWidth + ox
        val y = this.y + padding.top + yi * lineHeight - extraScrolling
        val tw = getTextSizeX(font, text, -1, -1)
        if (backgroundColor != this.backgroundColor) {
            drawRect(x, y.toInt(), charWidth, lineHeight, background or DefaultStyle.black)
        }
        drawText(x - (charWidth - tw) / 2, y.toInt(), font, key, textColor, background and 0xffffff)
    }

    private val cacheSize = 128
    private val textCacheKeyCache = arrayOfNulls<TextCacheKey>(cacheSize)
    private val textSizes = IntArray(cacheSize)

    companion object {

        private const val sectionSize = 4096
        private const val timeout = 5_000L
        private val cache = CacheSection("ByteSections")

        val displayedBytes = CharArray(256) { '.' }

        init {
            val d = displayedBytes
            for (c in 'A'..'Z') d[c.code] = c
            for (c in 'a'..'z') d[c.code] = c
            for (c in '0'..'9') d[c.code] = c
            for (c in "@^°,-_:;`~'!\"§$%&/()=?{[]}\\+-*/|<>") d[c.code] = c
        }

        fun get(file: FileReference, index: Long, async: Boolean): ByteArray? {
            val data = cache.getEntry(Triple(file, file.lastModified, index), timeout, async) { (file1, _, index1) ->
                val bytes = file1.inputStream()
                    .use {
                        it.skipN(index1 * sectionSize)
                            .readNBytes2(sectionSize, ByteArray(sectionSize), false)
                    }
                CacheData(bytes)
            } as? CacheData<*>
            return data?.value as? ByteArray
        }

        @JvmStatic
        fun main(args: Array<String>) {
            testUI {
                StudioBase.instance?.setVsyncEnabled(false)
                ScrollPanelY(HexEditor(style).apply {
                    file = FileReference.getReference("E:/MacOS/macos.qcow2")
                }, Padding.Zero, style, AxisAlignment.CENTER)
            }
        }
    }

}