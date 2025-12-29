package me.anno.fonts

import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.getSize
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.saveable.Saveable
import me.anno.utils.types.AnyToBool.getBool
import me.anno.utils.types.AnyToFloat.getFloat
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toIntOr
import kotlin.math.ceil

/**
 * Rem's Engine's standard font class,
 * has the standard bold/italic,
 * but it also has relative tab size, char spacing, a mono-flag, and line spacing options.
 *
 * Each instance should be handled as immutable.
 * (It is mutable only for deserialization)
 * */
class Font(
    name: String, size: Float,
    isBold: Boolean, isItalic: Boolean,
    relativeTabSize: Float, relativeCharSpacing: Float,
    isEqualSpaced: Boolean, relativeLineSpacing: Float,
) : Saveable() {

    constructor() : this("Verdana", 24, false, false)
    constructor(name: String, size: Int) : this(name, size, isBold = false, isItalic = false)
    constructor(name: String, size: Float) : this(name, size, isBold = false, isItalic = false)
    constructor(name: String, size: Float, isBold: Boolean, isItalic: Boolean) : this(
        name, size, isBold, isItalic, 4f, 0f,
        false, 1.5f
    )

    constructor(source: FileReference, size: Int) : this(source.absolutePath, size)
    constructor(source: FileReference, size: Float) : this(source.absolutePath, size)

    constructor(name: String, size: Int, isBold: Boolean, isItalic: Boolean) :
            this(name, size.toFloat(), isBold, isItalic)

    class SampleSize(font: Font) {
        val size = DrawTexts.getTextSize(font, "x", -1, -1)
        val width = GFXx2D.getSizeX(size)
        val height = GFXx2D.getSizeY(size)
    }

    var name = name
        private set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var size = size
        private set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var isBold = isBold
        private set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var isItalic = isItalic
        private set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var isEqualSpaced = isEqualSpaced
        private set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var relativeLineSpacing = relativeLineSpacing
        private set(value) {
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
        private set

    /**
     * Extra space between the characters, relative to font.size;
     * 0f is the default, > 0 means the characters are farther from each other and < 0 means they are closer together
     * */
    var relativeCharSpacing: Float = relativeCharSpacing
        private set(value) {
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

    /**
     * How much bigger each line shall be in pixels.
     * */
    val lineSpacingI: Int
        get() = (size * relativeLineSpacing).toIntOr()

    val emptySize: Int get() = getSize(emptyWidth, lineHeightI)

    @Suppress("unused")
    val sampleSize get() = sample.value.size

    fun invalidate() {
        sample = lazy { SampleSize(this) }
    }

    fun withBold(isBold: Boolean) =
        if (isBold == this.isBold) this
        else Font(
            name, size, isBold, isItalic,
            relativeTabSize, relativeCharSpacing,
            isEqualSpaced,
            relativeLineSpacing,
        )

    fun withItalic(isItalic: Boolean) =
        if (isItalic == this.isItalic) this
        else Font(
            name, size, isBold, isItalic,
            relativeTabSize, relativeCharSpacing,
            isEqualSpaced,
            relativeLineSpacing,
        )

    fun withName(name: String) =
        if (name == this.name) this
        else Font(
            name, size, isBold, isItalic,
            relativeTabSize, relativeCharSpacing,
            isEqualSpaced,
            relativeLineSpacing,
        )

    fun withSize(size: Float) =
        if (size == this.size) this
        else Font(
            name, size, isBold, isItalic,
            relativeTabSize, relativeCharSpacing,
            isEqualSpaced,
            relativeLineSpacing,
        )

    @Suppress("unused")
    fun withRelativeTabSize(relativeTabSize: Float) =
        if (relativeTabSize == this.relativeTabSize) this
        else Font(
            name, size, isBold, isItalic,
            relativeTabSize, relativeCharSpacing,
            isEqualSpaced,
            relativeLineSpacing,
        )

    @Suppress("unused")
    fun withRelativeCharSpacing(relativeCharSpacing: Float) =
        if (relativeCharSpacing == this.relativeCharSpacing) this
        else Font(
            name, size, isBold, isItalic,
            relativeTabSize, relativeCharSpacing,
            isEqualSpaced,
            relativeLineSpacing,
        )

    @Suppress("unused")
    fun withEqualSpaced(equalSpaced: Boolean) =
        if (equalSpaced == this.isEqualSpaced) this
        else Font(
            name, size, isBold, isItalic,
            relativeTabSize, relativeCharSpacing,
            equalSpaced,
            relativeLineSpacing,
        )

    @Suppress("unused")
    fun withRelativeLineSpacing(relativeLineSpacing: Float) =
        if (relativeLineSpacing == this.relativeLineSpacing) this
        else Font(
            name, size, isBold, isItalic,
            relativeTabSize, relativeCharSpacing,
            isEqualSpaced,
            relativeLineSpacing,
        )

    override fun equals(other: Any?): Boolean {
        if (other !is Font) return false
        return other.isBold == isBold && other.isItalic == isItalic && other.name == name && other.size == size
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + isBold.hashCode()
        result = 31 * result + isItalic.hashCode()
        result = 31 * result + relativeTabSize.hashCode()
        result = 31 * result + relativeCharSpacing.hashCode()
        result = 31 * result + isEqualSpaced.hashCode()
        result = 31 * result + relativeLineSpacing.hashCode()
        return result
    }

    override fun toString(): String {
        val flags = if (isBold) if (isItalic) " bold italic" else " bold" else if (isItalic) " italic" else ""
        return "$name $size$flags, $relativeTabSize tabs, $relativeCharSpacing rcs, $relativeLineSpacing rls"
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("name", name)
        writer.writeFloat("size", size)
        writer.writeBoolean("isBold", isBold)
        writer.writeBoolean("isItalic", isItalic)
        writer.writeFloat("charSpacing", relativeCharSpacing)
        writer.writeFloat("tabSize", relativeTabSize)
        writer.writeBoolean("equalSpaced", isEqualSpaced)
        writer.writeFloat("lineSpacing", relativeLineSpacing)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "name" -> this.name = value.toString()
            "size" -> size = getFloat(value)
            "isBold" -> isBold = getBool(value)
            "isItalic" -> isItalic = getBool(value)
            "charSpacing" -> relativeCharSpacing = getFloat(value)
            "tabSize" -> relativeTabSize = getFloat(value)
            "equalSpaced" -> isEqualSpaced = getBool(value)
            "lineSpacing" -> relativeLineSpacing = getFloat(value)
            else -> super.setProperty(name, value)
        }
    }
}