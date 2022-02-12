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

    var backgroundColor = 0

    var numbersColor = -1
    var numbersBGColor = 0
    var numbersLineColor = 0x770000

    var selectedLineBGColor = 0
    var selectedBGColor = 0x333333
    var selectedSingleColor = 0x666666
    var selectedMultipleColor = 0x999999

    var cursorColor = 0x990000

    var supportsStyleAlpha = false

    override fun save(writer: BaseWriter) {
        super.save(writer)
        if (supportsStyleAlpha) {
            if (styles.all { it.isSimple() }) {
                writer.writeIntArray("styles", IntArray(styles.size) { styles[it].color })
            } else {
                writer.writeObjectArray(null, "styles", styles)
            }
        } else {
            writer.writeIntArray("s", IntArray(styles.size) { styles[it].encode() })
        }
        writer.writeInt("bg", backgroundColor, true)
        writer.writeInt("nCol", numbersColor, true)
        writer.writeInt("nBG0", numbersBGColor, true)
        writer.writeInt("nLCol", numbersLineColor, true)
        writer.writeInt("selBG", selectedBGColor, true)
        writer.writeInt("selSin", selectedSingleColor, true)
        writer.writeInt("selMul", selectedMultipleColor, true)
        writer.writeInt("cursor", cursorColor, true)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "background", "bg" -> backgroundColor = value
            "numbersColor", "nCol" -> numbersColor = value
            "numbersOddBG", "nBG0", "nBG", "nBG1" -> numbersBGColor = value
            "numbersLineColor", "nLCol" -> numbersLineColor = value
            "selectedBG", "selBG" -> selectedBGColor = value
            "selectedSingle", "selSin" -> selectedSingleColor = value
            "selectedMultiple", "selMul" -> selectedMultipleColor = value
            "cursorColor", "cursor" -> cursorColor = value
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

    override val className: String = "LanguageTheme"

}