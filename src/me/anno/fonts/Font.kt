package me.anno.fonts

import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.GFXx2D
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import kotlin.math.roundToInt

class Font(name: String, size: Float, isBold: Boolean, isItalic: Boolean) : Saveable() {

    constructor() : this("Verdana", 24, false, false)
    constructor(name: String, size: Int) : this(name, size, isBold = false, isItalic = false)
    constructor(name: String, size: Float) : this(name, size, isBold = false, isItalic = false)
    constructor(source: FileReference, size: Int) : this(source.absolutePath, size)
    constructor(source: FileReference, size: Float) : this(source.absolutePath, size)

    constructor(name: String, size: Int, isBold: Boolean, isItalic: Boolean) :
            this(name, size.toFloat(), isBold, isItalic)

    class SampleSize(font: Font) {
        val size = DrawTexts.getTextSize(font, "x", -1, -1, false)
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

    val sizeInt get() = size.roundToInt()
    val sizeIndex get() = FontManager.getFontSizeIndex(size)

    var sample = lazy { SampleSize(this) }
    val sampleWidth get() = sample.value.width
    val sampleHeight get() = sample.value.height

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
        "$name $size${if (isBold) if (isItalic) " bold italic" else " bold" else if (isItalic) " italic" else ""}"

    override fun readFloat(name: String, value: Float) {
        if (name == "size") size = value
        else super.readFloat(name, value)
    }

    override fun readString(name: String, value: String) {
        if (name == "name") this.name = value
        else super.readString(name, value)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "isBold" -> isBold = value
            "isItalic" -> isItalic = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("name", name)
        writer.writeFloat("size", size)
        writer.writeBoolean("isBold", isBold)
        writer.writeBoolean("isItalic", isItalic)
    }

}