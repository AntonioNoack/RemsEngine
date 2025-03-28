package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.engine.WindowRenderFlags
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.debug.TestEngine.Companion.testUI2
import me.anno.ui.editor.code.CodeEditor
import me.anno.ui.editor.code.codemirror.LanguageThemeLib
import me.anno.ui.input.EnumInput

fun themeInput(editor: CodeEditor): Panel {
    return EnumInput(
        NameDesc("Theme"), NameDesc(editor.theme.name),
        LanguageThemeLib.listOfAll.map { NameDesc(it.name) }, style
    ).setChangeListener { _, index, _ ->
        editor.theme = LanguageThemeLib.listOfAll[index]
    }
}

fun wrapScrolling(editor: CodeEditor): Panel {
    return ScrollPanelY(editor.fill(1f), style).fill(1f)
}

fun testCodeEditor(title: String, editor: CodeEditor) {
    disableRenderDoc()
    testUI2(title) {

        WindowRenderFlags.enableVSync = true

        CodeEditor.registerActions()

        listOf(
            themeInput(editor),
            wrapScrolling(editor)
        )
    }
}

fun main() {
    val editor = CodeEditor(style)
    editor.setText(
        "" +
                "if cnt == 1 and state == 0 then\n" +
                "  print('this is cool')\n" +
                "end\n" +
                "while 1 == 1\n" +
                "do\n" +
                "   print('infinity')\n" +
                "end\n" +
                " 4\n 5\n 6\n 7\n 8\n 9\n10\n"
    )
    testCodeEditor("Code Editor Lua", editor)
}