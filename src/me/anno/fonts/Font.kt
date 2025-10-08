package me.anno.fonts

import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.saveable.Saveable
import me.anno.utils.types.AnyToFloat.getFloat
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toIntOr
import kotlin.math.ceil

class Font(
    name: String, size: Float,
    isBold: Boolean, isItalic: Boolean,
    relativeTabSize: Float, relativeCharSpacing: Float
) : Saveable() {

    constructor() : this("Verdana", 24, false, false)
    constructor(name: String, size: Int) : this(name, size, isBold = false, isItalic = false)
    constructor(name: String, size: Float) : this(name, size, isBold = false, isItalic = false)
    constructor(name: String, size: Float, isBold: Boolean, isItalic: Boolean) :
            this(name, size, isBold, isItalic, 4f, 0f)

    constructor(source: FileReference, size: Int) : this(source.absolutePath, size)
    constructor(source: FileReference, size: Float) : this(source.absolutePath, size)

    constructor(name: String, size: Int, isBold: Boolean, isItalic: Boolean) :
            this(name, size.toFloat(), isBold, isItalic)

    class SampleSize(font: Font) {
        val size = DrawTexts.getTextSize(font, "x", -1, -1).waitFor() ?: 0
        val width = GFXx2D.getSizeX(size)
        val height = GFXx2D.getSizeY(size)
    }

    var name = name
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var size = size
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var isBold = isBold
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var isItalic = isItalic
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    // no influence on sample
    /**
     * How many spaces each tab shall be, typically 2, 4 or 8.
     * The default shall be 4f
     * */
    var relativeTabSize: Float = relativeTabSize

    /**
     * Extra space between the characters, relative to font.size;
     * 0f is the default, > 0 means the characters are farther from each other and < 0 means they are closer together
     * */
    var relativeCharSpacing: Float = relativeCharSpacing
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    val sizeInt get() = size.roundToIntOr()
    val sizeIndex get() = FontManager.getFontSizeIndex(size)

    var sample = lazy { SampleSize(this) }
    val sampleWidth get() = sample.value.width
    val sampleHeight get() = sample.value.height

    /**
     * Width of an empty string
     * */
    val emptyWidth: Int get() = 2

    /**
     * The size that will be used for texture generation.
     * +1 for padding, that will be applied;
     *
     * This is lazy, because we need for our Mods to be loaded for FontManager to be available.
     * */
    val lineHeightI by lazy { ceil(FontManager.getLineHeight(this)).toIntOr() + 1 }

    val emptySize: Int get() = getSize(emptyWidth, lineHeightI)

    @Suppress("unused")
    val sampleSize get() = sample.value.size

    fun invalidate() {
        sample = lazy { SampleSize(this) }
    }

    fun withBold(bold: Boolean) = if (bold == isBold) this else Font(name, size, bold, isItalic)
    fun withItalic(italic: Boolean) = if (italic == isItalic) this else Font(name, size, isBold, italic)
    fun withName(name: String) = if (name == this.name) this else Font(name, size, isBold, isItalic)
    fun withSize(size: Float) = if (size == this.size) this else Font(name, size, isBold, isItalic)

    override fun equals(other: Any?): Boolean {
        if (other !is Font) return false
        return other.isBold == isBold && other.isItalic == isItalic && other.name == name && other.size == size
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + isBold.hashCode()
        result = 31 * result + isItalic.hashCode()
        return result
    }

    override fun toString() =
        "$name $size${if (isBold) if (isItalic) " bold italic" else " bold" else if (isItalic) " italic" else ""}" +
                (", $relativeTabSize tabs, $relativeCharSpacing sp")

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("name", name)
        writer.writeFloat("size", size)
        writer.writeBoolean("isBold", isBold)
        writer.writeBoolean("isItalic", isItalic)
        writer.writeFloat("charSpacing", relativeCharSpacing)
        writer.writeFloat("tabSize", relativeTabSize)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "name" -> this.name = value.toString()
            "size" -> size = getFloat(value)
            "isBold" -> isBold = value == true
            "isItalic" -> isItalic = value == true
            "charSpacing" -> relativeCharSpacing = getFloat(value)
            "tabSize" -> relativeTabSize = getFloat(value)
            else -> super.setProperty(name, value)
        }
    }
}