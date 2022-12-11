package me.anno.ui.editor.code.codemirror

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import kotlin.math.min

class LanguageTheme(val styles: Array<LanguageStyle>) : Saveable() {

    constructor() : this(Array(TokenType.values2.size) { LanguageStyle() })
    constructor(base: LanguageStyle) : this(Array(TokenType.values2.size) { LanguageStyle(base) })

    fun getColor(tokenType: TokenType) = styles[tokenType.ordinal].color
    fun getSquiggles(tokenType: TokenType) = styles[tokenType.ordinal].squiggles
    fun getUnderlined(tokenType: TokenType) = styles[tokenType.ordinal].underlined
    fun getBold(tokenType: TokenType) = styles[tokenType.ordinal].bold
    fun getItalic(tokenType: TokenType) = styles[tokenType.ordinal].italic

    var name = ""

    var backgroundColor = 0

    var numbersColor = 0
    var numbersBGColor = 0
    var numbersLineColor = 0

    var selectedLineBGColor = 0
    var selectedBGColor = 0

    var matchingBracketColor = 0

    var cursorColor = 0

    var supportsStyleAlpha = false

    override fun save(writer: BaseWriter) {
        super.save(writer)
        if (supportsStyleAlpha) {
            if (styles.all { it.isSimple() }) {
                writer.writeColorArray("styles", IntArray(styles.size) { styles[it].color })
            } else {
                writer.writeObjectArray(null, "styles", styles)
            }
        } else {
            writer.writeColorArray("s", IntArray(styles.size) { styles[it].encode() })
        }
        writer.writeString("name", name)
        writer.writeColor("bg", backgroundColor)
        writer.writeColor("nCol", numbersColor)
        writer.writeColor("nBG0", numbersBGColor)
        writer.writeColor("nLCol", numbersLineColor)
        writer.writeColor("selBG", selectedBGColor)
        writer.writeColor("mbc", matchingBracketColor)
        writer.writeColor("cursor", cursorColor)
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "name" -> this.name = value ?: return
            else -> super.readString(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "background", "bg" -> backgroundColor = value
            "numbersColor", "nCol" -> numbersColor = value
            "numbersOddBG", "nBG0", "nBG", "nBG1" -> numbersBGColor = value
            "numbersLineColor", "nLCol" -> numbersLineColor = value
            "selectedBG", "selBG" -> selectedBGColor = value
            "cursorColor", "cursor" -> cursorColor = value
            "matchingBracketColor", "mbc" -> matchingBracketColor = value
            else -> super.readInt(name, value)
        }
    }

    override fun readIntArray(name: String, values: IntArray) {
        when (name) {
            "styles" -> {
                for (i in 0 until min(values.size, styles.size)) {
                    styles[i] = LanguageStyle(values[i])
                }
            }
            "s" -> {
                for (i in 0 until min(values.size, styles.size)) {
                    styles[i].decode(values[i])
                }
            }
            else -> super.readIntArray(name, values)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        if (name == "styles") {
            for (i in values.indices) {
                styles[i] = values[i] as? LanguageStyle ?: continue
            }
        } else super.readObjectArray(name, values)
    }

    override val className get() = "LanguageTheme"

}