package me.anno.config

import me.anno.fonts.FontStats
import me.anno.io.config.ConfigBasics
import me.anno.io.files.InvalidRef
import me.anno.io.utils.StringMap
import me.anno.ui.Style
import me.anno.utils.Color.black
import me.anno.utils.Color.white

object DefaultStyle {

    const val nearWhite = black or 0xdddddd
    const val lightGray = black or 0xcccccc
    const val midGray = black or 0xaaaaaa
    const val deepDark = black or 0x2b2b2b
    const val reallyDark = black or 0x151515
    const val flatDark = black or 0x3c3f41
    const val scrollGray = black or 0x595b5d
    const val iconGray = black or 0xafb1b3

    const val fontGray = black or 0xbbbbbb

    const val shinyBlue = black or 0x4986f5
    const val brightYellow = black or 0xffba50

    val baseTheme = Style(null, null)

    init {
        initDefaults()
    }

    fun initDefaults() {

        val fontSize = FontStats.getDefaultFontSize()
        default("fontName", "Verdana")
        default("fontSize", fontSize)

        // light / dark
        default("small.textColor", fontGray, black)
        default("header.text.fontItalic", true)
        default("textColor", fontGray, black)
        default("options.textColor", fontGray, black)
        default("italic.propertyInspector.textColor", fontGray, black)
        default("link.textColor", shinyBlue, brightYellow)

        default("textColorFocused", white, shinyBlue)

        // dark / light
        default("background", flatDark, white)
        default("menu.background", black, white)
        default("tooltip.background", black, white)
        default("treeView.background", flatDark, midGray)
        default("propertyInspector.background", flatDark, midGray)
        default("sceneView.background", deepDark, lightGray)
        default("menu.background", reallyDark, nearWhite)
        default("spacer.background", deepDark, lightGray)
        default("spacer.menu.background", fontGray, lightGray)
        default("options.spacer.background", flatDark, midGray)
        default("deep.background", deepDark, lightGray)
        default("deep.edit.background", deepDark, lightGray)
        default("deep.propertyInspector.background", deepDark, lightGray)
        default("spacer.background", scrollGray, white)

        // special color
        default("accentColor", brightYellow, shinyBlue)

        default("spacer.width", 0)
        default("spacer.menu.width", 1)
        default("treeView.inset", fontSize / 2)

        default("textPadding", 2)

        for ((key, value) in loadStyle("style.config")) {
            baseTheme.values[key, value]
        }
    }

    private fun default(key: String, both: Any) {
        default(key, both, both)
    }

    private fun default(key: String, dark: Any, light: Any) {
        baseTheme.values["$key.dark", dark]
        baseTheme.values["$key.light", light]
    }

    fun loadStyle(path: String): StringMap {
        return ConfigBasics.loadConfig(path, InvalidRef, baseTheme.values, true)
    }
}