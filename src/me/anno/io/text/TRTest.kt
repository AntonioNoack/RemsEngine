package me.anno.io.text

import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.ui.editor.code.codemirror.LanguageStyle

fun main() {
    registerCustomClass(LanguageStyle())
    val style1 = LanguageStyle()
    style1.color = 0x775533
    val text = TextWriter.toText(style1)
    val style2 = TextReader.readFirst<LanguageStyle>(text)!!
    if (style2.color != style1.color) throw RuntimeException("$text -> " + style2.color.toString(16))
}