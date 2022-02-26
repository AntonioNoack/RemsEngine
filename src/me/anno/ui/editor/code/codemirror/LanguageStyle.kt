package me.anno.ui.editor.code.codemirror

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.utils.Color.toHexColor
import me.anno.utils.types.Booleans.toInt

open class LanguageStyle(
    var color: Int,
    var squiggles: Boolean,
    var underlined: Boolean,
    var bold: Boolean,
    var italic: Boolean
) : Saveable() {

    constructor(color: Int) : this(color, false, false, false, false)
    constructor() : this(0, false, false, false, false)
    constructor(base: LanguageStyle) : this(
        base.color,
        base.squiggles, base.underlined,
        base.bold, base.italic
    )

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeColor("color", color, true)
        writer.writeBoolean("squiggles", squiggles, false)
        writer.writeBoolean("underlined", underlined, false)
        writer.writeBoolean("bold", bold, false)
        writer.writeBoolean("italic", italic, false)
    }

    fun encode(): Int {
        return color.and(0xffffff) + (squiggles.toInt(8) + underlined.toInt(4) + bold.toInt(2)
                + italic.toInt(1)).shl(24)
    }

    fun decode(v: Int) {
        color = v or (255 shl 24)
        squiggles = v.and(1 shl 27) > 0
        underlined = v.and(1 shl 26) > 0
        bold = v.and(1 shl 25) > 0
        italic = v.and(1 shl 24) > 0
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "color" -> color = value
            else -> super.readInt(name, value)
        }
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "squiggles" -> squiggles = value
            "underlined" -> underlined = value
            "bold" -> bold = value
            "italic" -> italic = value
            else -> super.readBoolean(name, value)
        }
    }

    fun isSimple() = !bold && !italic && !squiggles && !underlined

    override fun equals(other: Any?): Boolean {
        return other is LanguageStyle &&
                other.color == color &&
                other.underlined == underlined &&
                other.squiggles == squiggles &&
                other.bold == bold &&
                other.italic == italic
    }

    override fun hashCode(): Int {
        return color.hashCode() * 31 + (underlined.toInt(8) + squiggles.toInt(4)
                + bold.toInt(2) + italic.toInt(1)).hashCode()
    }

    override val className: String = "LanguageStyle"
}