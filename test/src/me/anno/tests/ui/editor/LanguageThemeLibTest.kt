package me.anno.tests.ui.editor

import me.anno.ui.editor.code.codemirror.LanguageThemeLib
import org.junit.jupiter.api.Test

class LanguageThemeLibTest {
    @Test
    fun testThemeLib() {
        // was crashing JVM2WASM, because my Char.digit()-implementation
        // wasn't quite correct for hexadecimal values
        LanguageThemeLib.listOfAll
    }
}