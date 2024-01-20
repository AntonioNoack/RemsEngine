package me.anno.tests.utils

import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.utils.StringMap
import me.anno.utils.Color.mixARGB
import me.anno.ui.editor.code.codemirror.LanguageStyle
import me.anno.ui.editor.code.codemirror.LanguageTheme
import me.anno.ui.editor.code.codemirror.LanguageThemeLib.base
import me.anno.ui.editor.code.tokenizer.TokenType
import me.anno.utils.Color.a
import me.anno.utils.ColorParsing
import me.anno.utils.OS
import me.anno.utils.strings.StringHelper.titlecase
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

fun main() {

    registerCustomClass(StringMap())

    val folder = OS.downloads.getChild("codemirror/theme")
    fun extractColor(v0: String): Int? {
        val v = v0
            .replace("!important", "")
            .replace("1px", "")
            .replace("solid", "")
            .trim()
        return try {
            when {
                "rgb" in v -> ColorParsing.parseColor(v.substring(v.indexOf("rgb")))
                "#" in v -> ColorParsing.parseColor(v.substring(v.indexOf('#')))
                else -> ColorParsing.parseColor(v)
            }
        } catch (e: Exception) {
            null
        }
    }

    val tokenTypeQualifiers = TokenType.entries.map {
        ".cm-" + when (it) {
            TokenType.DEFINE -> "def"
            TokenType.STRING2 -> "string-2"
            TokenType.VARIABLE2 -> "variable-2"
            TokenType.VARIABLE3 -> "variable-3"
            else -> it.name.lowercase()
        }
    }

    val listOfAll = ArrayList<String>()
    for (file in folder.listChildren()) {
        if (file.lcExtension == "css" && file.nameWithoutExtension != "ambiance-mobile") {
            var name = file.nameWithoutExtension.trim()
            val text = file.readTextSync()
            var index = 0
            var baseColor = 0
            var baseBGColor = -1
            fun String.removeSpaces() =
                this.replace('\r', ' ')
                    .replace('\n', ' ')
                    .replace('\t', ' ')
                    .replace("    ", " ")
                    .replace("    ", " ")
                    .replace("  ", " ")
                    .replace("  ", " ")
                    .trim()
            // format name into kotlin-compatible name
            if (name[0] in '0'..'9') name = "Style$name"
            name = name.split('-').joinToString("") { it.titlecase() }
            print("val $name = read(\"")
            listOfAll.add(name)
            val styles = Array(TokenType.entries.size) { LanguageStyle() }
            val theme = LanguageTheme(styles)
            while (true) {

                val nextIndex = text.indexOf('{', index)
                if (nextIndex < 0) break

                val nextIndex2 = text.indexOf('}', nextIndex + 1)
                if (nextIndex2 < 0) break

                val keys = text.substring(index, nextIndex)
                    .removeSpaces()
                    .split(' ')
                    .map {
                        it
                            .replace("span", "")
                            .replace("div", "")
                            .trim()
                    }

                val matchingTokens = tokenTypeQualifiers
                    .withIndex()
                    .filter { it.value in keys }
                    .map { it.index }

                fun forEachCSS(action: (key: String, value: String) -> Unit) {
                    val keyValuePairs = text.substring(nextIndex + 1, nextIndex2)
                        .split(';').map { it.trim() }
                    for (keyValue in keyValuePairs) {
                        val keyIndex = keyValue.indexOf(':')
                        if (keyIndex > 0) {
                            val key = keyValue.substring(0, keyIndex).trim().lowercase()
                            val value = keyValue.substring(keyIndex + 1)
                                .split("/*")[0]
                                .trim()
                                .lowercase()
                            action(key, value)
                        }
                    }
                }

                if (matchingTokens.isNotEmpty()) {
                    fun forEachStyle(consumer: (LanguageStyle) -> Unit) {
                        for (token in matchingTokens) {
                            val style = styles[token]
                            consumer(style)
                        }
                    }
                    // span.cm-keyword
                    forEachCSS { key, value ->
                        when (key) {
                            "color" -> {
                                val v = extractColor(value)
                                if (v != null) forEachStyle { it.color = v }
                            }
                            "font-weight" -> {
                                if (value == "bold") {
                                    forEachStyle { it.bold = true }
                                }
                            }
                            "font-style" -> {
                                if (value == "italic") {
                                    forEachStyle { it.italic = true }
                                }
                            }
                            "text-decoration" -> {
                                if (value == "underline") {
                                    forEachStyle { it.underlined = true }
                                }
                            }
                        }
                    }
                }
                forEachCSS { key, value ->
                    for (key0 in keys) {
                        when (key0) {
                            ".CodeMirror-matchingbracket" -> {
                                when (key) {
                                    "color" -> {
                                        val c = extractColor(value)
                                        if (c != null) theme.matchingBracketColor = c
                                    }
                                }
                            }
                            ".CodeMirror-activeline-background" -> {
                                when (key) {
                                    "background", "background-color" -> {
                                        val c = extractColor(value)
                                        if (c != null) theme.selectedLineBGColor = c
                                    }
                                }
                            }
                            ".CodeMirror-gutters" -> {
                                when (key) {
                                    "background", "background-color" -> {
                                        val c = extractColor(value)
                                        if (c != null) theme.numbersBGColor = c
                                    }
                                    "border-right" -> {
                                        val c = extractColor(value)
                                        if (c != null) theme.numbersLineColor = c
                                    }
                                }
                            }
                            ".CodeMirror-linenumber" -> {
                                when (key) {
                                    "color" -> {
                                        val c = extractColor(value)
                                        if (c != null) theme.numbersColor = c
                                    }
                                }
                            }
                            ".CodeMirror-selected" -> {
                                when (key) {
                                    "background", "background-color" -> {
                                        val c = extractColor(value)
                                        if (c != null) theme.selectedBGColor = c
                                    }
                                }
                            }
                            ".CodeMirror-cursor" -> {
                                when (key) {
                                    "border-left" -> {
                                        val c = extractColor(value)
                                        if (c != null) theme.cursorColor = c
                                    }
                                }
                            }

                            else -> {
                                if (key0.startsWith(".cm-s-") && key0.endsWith(".CodeMirror")) {
                                    when (key) {
                                        "color" -> {
                                            val c = extractColor(value)
                                            if (c != null) baseColor = c
                                        }
                                        "background", "background-color" -> {
                                            val c = extractColor(value)
                                            if (c != null) {
                                                baseBGColor = c
                                                theme.backgroundColor = c
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                index = nextIndex2 + 1
            }
            if (theme.selectedBGColor == 0) {
                theme.selectedBGColor = theme.selectedLineBGColor
            }
            if (baseBGColor != 0) {
                for (style in styles) {
                    if (style.color.a() == 0) {
                        style.color = baseColor
                    } else if (style.color.a() != 255) {
                        style.color = mixARGB(
                            baseBGColor, style.color,
                            style.color.a() / 255f
                        ) or (255 shl 24)
                    }
                }
                for (p in theme::class.memberProperties) {
                    var color = p.getter.call(theme)
                    if (color is Int && color.a() != 255) {
                        color = mixARGB(
                            baseBGColor, color,
                            color.a() / 255f
                        ) or (255 shl 24)
                        if (p is KMutableProperty1<*, *>) {
                            p.setter.call(theme, color)
                        }
                    }
                }
            }
            theme.name = name
            print(
                JsonStringWriter.toText(theme, InvalidRef)
                    .substring(base.length)
                    .replace("\"", "'")
                    .replace("\n", "\"+\n\"")
            )
            println("\")")
        }
    }
    println("val listOfAll = listOf(${listOfAll.joinToString()})")
}