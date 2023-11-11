package me.anno.tests.ui

import me.anno.config.DefaultConfig
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.Language
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase
import me.anno.ui.debug.TestStudio.Companion.testUI2
import me.anno.ui.editor.code.CodeEditor
import me.anno.ui.editor.code.codemirror.LanguageThemeLib
import me.anno.ui.input.EnumInput

fun main() {
    disableRenderDoc()
    testUI2("Code Editor") {

        val instance = StudioBase.instance
        if (instance != null) {
            instance.enableVSync = true
            instance.language = Language.AmericanEnglish
        }

        CodeEditor.registerActions()

        val editor = CodeEditor(DefaultConfig.style)
        editor.setText(
            "" +
                    "if cnt == 1 and state == 0 then\n" +
                    "  print('this is cool')\n" +
                    "end\n" +
                    " 4\n 5\n 6\n 7\n 8\n 9\n10\n"
        )

        listOf(
            EnumInput(
                "Theme", "", editor.theme.name,
                LanguageThemeLib.listOfAll.map { NameDesc(it.name) }, DefaultConfig.style
            ).setChangeListener { _, index, _ ->
                editor.theme = LanguageThemeLib.listOfAll[index]
            },
            editor,
        )
    }
}