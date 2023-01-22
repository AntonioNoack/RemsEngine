package me.anno.ui.editor.code

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig.style
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFXBase
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.xml.XMLReader.skipN
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mixARGB
import me.anno.studio.StudioBase
import me.anno.ui.Panel
import me.anno.ui.base.Font
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.scrolling.LongScrollable
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.style.Style
import me.anno.utils.Color.a
import me.anno.utils.Color.black
import me.anno.utils.Color.hex4
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.desktop
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.Floats.fp16ToFP32
import me.anno.utils.types.InputStreams.readNBytes2
import org.apache.logging.log4j.LogManager
import java.io.RandomAccessFile
import java.math.BigInteger
import kotlin.math.ceil
import kotlin.math.log2

// done idea: move inFocus into WindowStack, because it really depends only on the stack

// hex editor like HxD, but better comparison mode
// maybe next to each other?, and then synchronize scroll lists? lock/unlock them? :)

// comparing stuff
// todo edit bytes
//  - mark changed byte sequences with different color
// todo undo/redo
// todo delete change sequences
// todo paste byte sequence
// -> saving
// data inspector by tooltip text (base 10, fp16, fp32, fp64, ... le/be)

class HexEditor(style: Style) : Panel(style), LongScrollable {

    var file: FileReference = InvalidRef

    val padding = Padding(4)
    var font = Font("Courier New", 16)

    var bytesPerLine = 16
    var spacing = 0.5f

    val charWidth get() = font.sampleWidth
    val lineHeight get() = font.sampleHeight

    var cursor0 = -1L
    var cursor1 = -1L

    var showAddress = true
    var addressDigits = 0

    // should be sorted, non-overlapping
    class ByteSequence(val offset: Long, val bytes: ByteArray)

    val changes = ArrayList<ByteSequence>()

    val compareTo = ArrayList<FileReference>()

    fun saveChanges(clear: Boolean) {
        if (changes.isEmpty()) return
        RandomAccessFile(file.absolutePath, "w").use { raf ->
            for (sequence in changes) {
                raf.seek(sequence.offset) // Go to byte at offset position 5.
                raf.write(sequence.bytes) // Write byte 70 (overwrites original byte at this offset).
            }
        }
        file.invalidate()
        if (clear) changes.clear()
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return if (action == "Save") {
            saveChanges(true)
            true
        } else super.onGotAction(x, y, dx, dy, action, isContinuous)
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        super.onDoubleClick(x, y, button)
        // todo find which byte is clicked,
        // todo collect input in hex or chars
        // todo then add to changed sequences
    }

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
        } else {
            addressDigits = 0
            minW += spacing
        }
        this.w = minW
        this.h = minH
    }

    override val canDrawOverBorders get() = true

    var textColor = white.withAlpha(0.8f)
    var textColorDifferent = mixARGB(textColor, 0xff0000 or black, 0.8f)
    var textColorSomeDifferent = mixARGB(textColor, 0xffff00 or black, 0.8f)
    var midLineColor = mixARGB(textColor, backgroundColor, 0.5f)
    var lineEveryN = 4
    var showText = true

    var selectedBackgroundColor = mixARGB(textColor, backgroundColor, 0.8f)

    var extraScrolling = 0L

    val spacing2 get() = (spacing * charWidth).toInt()
    val addressDx get() = spacing2 + charWidth * addressDigits

    private val buffers = ArrayList<ByteArray?>()
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
        val tcAD = textColorDifferent
        val tcSD = textColorSomeDifferent
        val addressDigits = addressDigits
        val spacing2 = spacing2
        val addressDx = addressDx
        // draw addresses
        val bytesPerLine = bytesPerLine
        for (lineNumber in l0 until l1) {
            val address = lineNumber * bytesPerLine
            for (digitIndex in 0 until addressDigits) {
                val digit = address.shr((addressDigits - 1 - digitIndex) * 4)
                // group these in pairs as well
                val pairOffset = if ((digitIndex + addressDigits).and(1) > 0) -1 else +1
                drawChar(digitIndex, lineNumber, pairOffset, hex4(digit), tc, bc)
            }
        }
        lateinit var buffer: ByteArray
        var lastBufferIndex = -1L
        // draw content
        val ox2 = bytesPerLine * spacing2 + addressDx
        val sbc = selectedBackgroundColor
        loop@ for (lineNumber in l0 until l1) {
            for (lineIndex in 0 until bytesPerLine) {
                val byteIndex = lineNumber * bytesPerLine + lineIndex
                val bufferIndex = byteIndex / sectionSize
                if (bufferIndex != lastBufferIndex) {
                    buffer = Companion.get(file, bufferIndex, false) ?: break@loop
                    buffers.clear()
                    for (i in compareTo.indices) {
                        buffers.add(Companion.get(compareTo[i], bufferIndex, false))
                    }
                    lastBufferIndex = bufferIndex
                }
                val localIndex = (byteIndex and (sectionSize - 1L)).toInt()
                if (localIndex >= buffer.size) break@loop
                val rawValue = buffer[localIndex]
                val value = rawValue.toInt() and 0xff

                val allSame = buffers.all { it == null || it.size <= localIndex || it[localIndex] == rawValue }
                val allDifferent = buffers.all { it == null || it.size <= localIndex || it[localIndex] != rawValue }

                val tc1 = when {
                    allSame -> tc
                    allDifferent -> tcAD
                    else -> tcSD
                }

                val ox = lineIndex * spacing2 + addressDx
                val isSelected = byteIndex in min(cursor0, cursor1)..max(cursor0, cursor1)

                val bc1 = if (isSelected) sbc else bc
                // draw hex
                drawChar(lineIndex * 2, lineNumber, ox + 1, hex4(value.shr(4)), tc1, bc1)
                drawChar(lineIndex * 2 + 1, lineNumber, ox - 1, hex4(value), tc1, bc1)
                if (showText) {
                    // draw byte as char
                    drawChar(bytesPerLine * 2 + lineIndex, lineNumber, ox2, displayedBytes[value], tc1, bc1)
                }
            }
        }
        // separation lines
        val yl0 = max(y0.toLong(), by)
        val yl1 = (min(y1.toLong(), by + lineCount * lineHeight) - yl0).toInt()
        val yl0i = yl0.toInt()
        val lineColor = mixARGB(backgroundColor, midLineColor, midLineColor.a() / 255f)
        for (i in 0 until bytesPerLine step lineEveryN) {
            val x2 = bx + addressDx + i * (spacing2 + 2 * charWidth) - (spacing2 + 1) / 2
            val lineColor2 = if (i > 0) mixARGB(backgroundColor, lineColor, 0.3f) else lineColor
            drawRect(x2, yl0i, 1, yl1, lineColor2)
        }
        val x2 = bx + addressDx + bytesPerLine * (spacing2 + 2 * charWidth) - (spacing2 + 1) / 2
        drawRect(x2, yl0i, 1, yl1, lineColor)
    }

    fun drawChar(
        xi: Int, yi: Long, dx: Int, char: Char,
        textColor: Int, backgroundColor: Int,
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
            val x = this.x + padding.left + xi * charWidth + dx
            val y = this.y + padding.top + yi * lineHeight - extraScrolling
            if (backgroundColor != this.backgroundColor) {
                drawRect(x, y.toInt(), charWidth, lineHeight, background or black)
            }
            drawText(x - (charWidth - tw) / 2, y.toInt(), font, key, textColor, background and 0xffffff)
        } else drawChar2(xi, yi, dx, char, textColor, backgroundColor)
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
            drawRect(x, y.toInt(), charWidth, lineHeight, background or black)
        }
        drawText(x - (charWidth - tw) / 2, y.toInt(), font, key, textColor, background and 0xffffff)
    }

    fun getCursorAt(x: Float, y: Float): Long {
        val iy = (y - (this.y + padding.top - extraScrolling)).toLong() / lineHeight
        val x0 = this.x + padding.left + addressDx
        val ix1 = (x - (x0)).toLong() / (charWidth * 2 + spacing2)
        val ix2 = (x - (x0 + bytesPerLine * spacing2)).toLong() / charWidth - bytesPerLine * 2
        val ix = if (ix1 < 16) max(ix1, 0) else min(ix2, 15)
        markedRight = ix1 >= 16
        return iy * bytesPerLine + ix
    }

    var markedRight = false
    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft) {
            cursor0 = getCursorAt(x, y)
            cursor1 = cursor0
            invalidateDrawing()
        }
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (isAnyChildInFocus && MouseButton.LEFT.id in Input.mouseKeysDown) {
            cursor1 = getCursorAt(x, y)
            invalidateDrawing()
        }
    }

    var isPastingAllowed = true
    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        if (isPastingAllowed) {
            val file = files.lastOrNull { it.exists && !it.isDirectory }
            if (file != null) {
                cursor0 = -1L
                cursor1 = -1L
                this.file = file
                invalidateDrawing()
            } else super.onPasteFiles(x, y, files)
        } else super.onPasteFiles(x, y, files)
    }

    fun getCopiedBytes(limit: Long): ByteArray? {
        val minIndex = max(min(cursor0, cursor1), 0)
        val maxIndex = min(max(cursor0, cursor1) + 1, file.length())
        if (minIndex >= maxIndex) return null
        if (maxIndex - minIndex > limit) {
            if (limit >= 2e9) LOGGER.warn("Cannot copy slices larger than 2 GiB")
            return null
        }
        val data = ByteArray((maxIndex - minIndex).toInt())
        var i = minIndex
        while (i < maxIndex) {
            val bufferIndex = i / sectionSize
            val startIndex = bufferIndex * sectionSize
            val endIndex = min(maxIndex, startIndex + sectionSize)
            val partData = Companion.get(file, bufferIndex, false)!!
            System.arraycopy(
                partData,
                (i - startIndex).toInt(), data,
                (i - minIndex).toInt(),
                (endIndex - i).toInt()
            )
            i = endIndex
        }
        return data
    }

    var copiedSeparator = ' '
    override fun onCopyRequested(x: Float, y: Float): Any? {
        val minIndex = max(min(cursor0, cursor1), 0)
        val maxIndex = min(max(cursor0, cursor1) + 1, file.length())
        if (maxIndex > minIndex) {
            val text = markedRight
            val limit = if (text) Int.MAX_VALUE.toLong() else (Int.MAX_VALUE / 3).toLong()
            val data = getCopiedBytes(limit) ?: return null
            // if was copied on right side, use string, else concat values
            // todo also mark this as being copied, to we could drop it into other hex editors
            return if (text) String(data) else {
                val builder = StringBuilder(3 * data.size - 1)
                val v0 = data[0].toInt()
                builder.append(hex4(v0.shr(4)))
                builder.append(hex4(v0.shr(4)))
                val sep = copiedSeparator
                for (j in 1 until data.size) {
                    builder.append(sep)
                    val v = data[j].toInt()
                    builder.append(hex4(v.shr(4)))
                    builder.append(hex4(v.shr(4)))
                }
                builder.toString()
            }
        } else return null
    }

    private val cacheSize = 128
    private val textCacheKeyCache = arrayOfNulls<TextCacheKey>(cacheSize)
    private val textSizes = IntArray(cacheSize)

    override fun getTooltipText(x: Float, y: Float): String? {
        val minIndex = max(min(cursor0, cursor1), 0)
        val maxIndex = min(max(cursor0, cursor1) + 1, file.length())
        if (maxIndex == minIndex) return null
        val limit = 16L
        if (maxIndex > minIndex + limit) {
            // idk what to do
            return (maxIndex - minIndex).formatFileSize()
        }
        val data = getCopiedBytes(limit) ?: return null
        val le = cursor0 > cursor1
        var bi = BigInteger.ZERO
        for (i in data.indices) {
            val j = if (le) i else data.size - 1 - i
            bi += BigInteger.valueOf(data[i].toLong().and(255)).shiftLeft(j * 8)
        }
        val unsigned = bi.toString()
        val msb = data[if (le) data.size - 1 else 0].toInt().hasFlag(128)
        val signed = if (msb) bi - BigInteger.ONE.shiftLeft(8 * data.size) else bi
        val prefix = if (le) "Little Endian" else "Big Endian"
        return when (data.size) {
            1 -> "U: $unsigned\nS: $signed\nB: ${bi.toString(2)}"
            2 -> "$prefix\nU: $unsigned\nS: $signed\nFP16: ${fp16ToFP32(bi.toInt())}"
            4 -> "$prefix\nU: $unsigned\nS: $signed\nFP32: ${Float.fromBits(bi.toInt())}"
            8 -> "$prefix\nU: $unsigned\nS: $signed\nFP64: ${Double.fromBits(bi.toLong())}"
            else -> "$prefix\nU: $unsigned\nS: $signed"
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(HexEditor::class)

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
                val bytes = file1.inputStreamSync()
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
            GFXBase.disableRenderDoc()
            testUI {
                StudioBase.instance?.enableVSync = false
                registerActions()
                val list = PanelListX(style)
                val files = listOf(
                    desktop.getChild("SM_Prop_Gem_03.prefab"),
                    desktop.getChild("SM_Env_Minetrack_Bridge_Broken_01.prefab"),
                    desktop.getChild("Character_Ghost_01.prefab"),
                    desktop.getChild("Character_Ghost_02.prefab"),
                    desktop.getChild("FX_Sword_Fire.prefab")
                )
                for (file1 in files) {
                    list.add(HexEditor(style).apply {
                        file = file1
                        compareTo.addAll(files)
                        compareTo.remove(file1)
                        showAddress = list.children.isEmpty()
                    })
                }
                ScrollPanelXY(list, Padding.Zero, style)
            }
        }

        @JvmStatic
        fun registerActions() {
            ActionManager.register("HexEditor.s.t.c", "Save")
        }

    }

}