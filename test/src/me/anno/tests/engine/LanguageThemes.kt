package me.anno.tests.engine

import me.anno.ui.editor.code.codemirror.LanguageThemeLib

fun main() {
    for (it in LanguageThemeLib.listOfAll) {
        println(it.name)
    }
}