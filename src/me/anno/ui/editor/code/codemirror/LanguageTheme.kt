package me.anno.ui.editor.code.codemirror

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.ui.editor.code.tokenizer.TokenType
import me.anno.utils.structures.lists.Lists.createArrayList
import kotlin.math.min

class LanguageTheme(val styles: ArrayList<LanguageStyle>) : Saveable() {

    constructor() : this(createArrayList(TokenType.entries.size, LanguageStyle()))

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
                writer.writeObjectList(null, "styles", styles)
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
        writer.writeColor("selLBG", selectedLineBGColor)
        writer.writeColor("mbc", matchingBracketColor)
        writer.writeColor("cursor", cursorColor)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "name" -> this.name = value as? String ?: return
            "background", "bg" -> backgroundColor = value as? Int ?: return
            "numbersColor", "nCol" -> numbersColor = value as? Int ?: return
            "numbersOddBG", "nBG0", "nBG", "nBG1" -> numbersBGColor = value as? Int ?: return
            "numbersLineColor", "nLCol" -> numbersLineColor = value as? Int ?: return
            "selectedBG", "selBG" -> selectedBGColor = value as? Int ?: return
            "selectedLineBG", "selLBG" -> selectedLineBGColor = value as? Int ?: return
            "cursorColor", "cursor" -> cursorColor = value as? Int ?: return
            "matchingBracketColor", "mbc" -> matchingBracketColor = value as? Int ?: return
            "styles" -> {
                when (value) {
                    is IntArray -> {
                        for (i in 0 until min(value.size, styles.size)) {
                            styles[i] = LanguageStyle(value[i])
                        }
                    }
                    is List<*> -> {
                        for (i in value.indices) {
                            styles[i] = value[i] as? LanguageStyle ?: continue
                        }
                    }
                }
            }
            "s" -> {
                val values = value as? IntArray ?: return
                for (i in 0 until min(values.size, styles.size)) {
                    styles[i].decode(values[i])
                }
            }
            else -> super.setProperty(name, value)
        }
    }
}