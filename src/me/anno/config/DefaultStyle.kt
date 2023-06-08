package me.anno.config

import me.anno.io.config.ConfigBasics
import me.anno.io.files.InvalidRef
import me.anno.io.utils.StringMap
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mixARGB
import me.anno.ui.style.Style
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.OS
import java.awt.Toolkit
import kotlin.system.exitProcess


@Suppress("unused")
object DefaultStyle {

    const val nearWhite = black or 0xdddddd
    const val lightGray = black or 0xcccccc
    const val midGray = black or 0xaaaaaa
    const val deepDark = black or 0x2b2b2b
    val reallyDark = mixARGB(black, deepDark, 0.5f)
    const val flatDark = black or 0x3c3f41
    const val scrollGray = black or 0x595b5d
    const val iconGray = black or 0xafb1b3

    const val fontGray = black or 0xbbbbbb

    const val shinyBlue = black or 0x4986f5
    const val brightYellow = black or 0xffba50

    val baseTheme = Style(null, null)

    init {

        val fontSize = clamp(Toolkit.getDefaultToolkit().screenSize.height / 80, 15, 60)

        set("fontName", "Verdana")
        set("fontSize", fontSize)

        // light / dark
        set("small.textColor", fontGray, black)
        set("textColor", fontGray, black)
        set("options.textColor", fontGray, black)
        set("italic.propertyInspector.textColor", fontGray, black)
        set("link.textColor", shinyBlue, brightYellow)

        set("textColorFocused", white, shinyBlue)

        // dark / light
        set("background", flatDark, white)
        set("menu.background", black, white)
        set("tooltip.background", black, white)
        set("treeView.background", flatDark, midGray)
        set("propertyInspector.background", flatDark, midGray)
        set("sceneView.background", deepDark, lightGray)
        set("menu.background", reallyDark, nearWhite)
        set("spacer.background", deepDark, lightGray)
        set("spacer.menu.background", fontGray, lightGray)
        set("options.spacer.background", flatDark, midGray)
        set("deep.background", deepDark, lightGray)
        set("deep.edit.background", deepDark, lightGray)
        set("deep.propertyInspector.background", deepDark, lightGray)
        set("spacer.background", scrollGray, white)

        // special color
        set("accentColor", brightYellow, shinyBlue)

        set("spacer.width", 0)
        set("spacer.menu.width", 1)
        set("treeView.inset", fontSize / 2)

        set("textPadding", 2)

        for ((key, value) in loadStyle("style.config")) {
            baseTheme.values[key, value]
        }
    }

    operator fun set(key: String, both: Any) {
        set(key, both, both)
    }

    operator fun set(key: String, dark: Any, light: Any) {
        baseTheme.values["$key.dark", dark]
        baseTheme.values["$key.light", light]
    }

    fun loadStyle(path: String): StringMap {
        return ConfigBasics.loadConfig(path, InvalidRef, baseTheme.values, true)
    }

}