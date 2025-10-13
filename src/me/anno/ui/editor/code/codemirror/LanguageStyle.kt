package me.anno.ui.editor.code.codemirror

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.types.Booleans.hasFlag
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

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "color" -> color = value as? Int ?: return
            "squiggles" -> squiggles = value == true
            "underlined" -> underlined = value == true
            "bold" -> bold = value == true
            "italic" -> italic = value == true
            else -> super.setProperty(name, value)
        }
    }

    fun encode(): Int {
        val flags = squiggles.toInt(FLAG_SQUIGGLES) +
                underlined.toInt(FLAG_UNDERLINED) +
                bold.toInt(FLAG_BOLD) +
                italic.toInt(FLAG_ITALIC)
        return color.and(0xffffff) + flags.shl(24)
    }

    fun decode(v: Int) {
        color = v or (255 shl 24)
        val flags = v ushr 24
        squiggles = flags.hasFlag(FLAG_SQUIGGLES)
        underlined = flags.hasFlag(FLAG_UNDERLINED)
        bold = flags.hasFlag(FLAG_BOLD)
        italic = flags.hasFlag(FLAG_ITALIC)
    }

    fun isSimple() = !bold && !italic && !squiggles && !underlined

    override fun equals(other: Any?): Boolean {
        return other is LanguageStyle && other.encode() == encode()
    }

    override fun hashCode(): Int {
        return encode().hashCode()
    }

    companion object {
        private const val FLAG_SQUIGGLES = 8
        private const val FLAG_UNDERLINED = 4
        private const val FLAG_BOLD = 2
        private const val FLAG_ITALIC = 1
    }
}