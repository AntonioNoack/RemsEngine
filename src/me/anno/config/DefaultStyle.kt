package me.anno.config

import me.anno.io.config.ConfigBasics
import me.anno.io.utils.StringMap
import me.anno.ui.style.Style
import me.anno.utils.mixARGB

object DefaultStyle {

    val black = 0xff000000.toInt()
    val deepDark = black or 0x2b2b2b
    val reallyDark = mixARGB(black, deepDark, 0.5f)
    val flatDark = black or 0x3c3f41
    val scrollGray = black or 0x595b5d
    val iconGray = black or 0xafb1b3

    val fontGray = black or 0xbbbbbb
    val white = -1

    val baseTheme = Style(null, null)
    val lightTheme = baseTheme.getStyle("light")
    val darkTheme = baseTheme.getStyle("dark")
    var defaultStyle = darkTheme

    init {
        val textSize = 15
        set("textFont", "Verdana")
        set("textSize", textSize)
        set("small.textSize", 12)
        set("small.textColor", fontGray and 0x7fffffff)
        set("textColor", black, fontGray)
        set("background", white, flatDark)
        set("menu.background", white, black)
        set("treeView.background", flatDark)
        set("propertyInspector.background", flatDark)
        set("sceneView.background", deepDark)
        set("menu.background", reallyDark)
        set("spacer.background", deepDark)
        set("spacer.menu.background", fontGray)
        set("spacer.width", 0)
        set("spacer.menu.width", 1)
        set("treeView.inset", textSize/2)
        set("options.spacer.background", flatDark)
        set("options.textColor", fontGray)
        set("deep.background", deepDark)
        set("deep.edit.background", deepDark)
        set("background.hover", mixARGB(iconGray, flatDark, 0.5f))
        set("textPadding", 2)
        set("deep.propertyInspector.background", deepDark)
        set("italic.propertyInspector.textItalic", true)
        set("italic.propertyInspector.textColor", fontGray and 0xafffffff.toInt())
        set("accentColor", black or 0xffba50)
        set("fileExplorer.textSize", 12)

        for((key, value) in loadStyle("style.config")){
            baseTheme.values[key] = value
        }
    }

    operator fun set(key: String, both: Any){
        set(key, both, both)
    }

    operator fun set(key: String, light: Any, dark: Any){
        baseTheme.values["$key.light"] = light
        baseTheme.values["$key.dark"] = dark
    }

    fun loadStyle(path: String): StringMap {
        return ConfigBasics.loadConfig(path, baseTheme.values, true)
    }

}