package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.ui.editor.code.CodeEditor
import me.anno.ui.editor.code.tokenizer.KotlinTokenizer

fun main() {
    // to do define syntax highlighting for Kotlin language :), and later Java, too
    val editor = CodeEditor(style)
    editor.language = KotlinTokenizer
    editor.setText(
        "" +
                "// normal comment\n" +
                "/* multi-line comment */\n" +
                "data class X(val v: Float, var y: Float)\n" +
                "fun main(x: Int){\n" +
                "   print(\"this is cool, name: \$name, time: \${Time.nanoTime}\")\n" +
                "   if(true) System.exit(-1);\n" +
                "}\n"
    )
    testCodeEditor("Code Editor Kotlin", editor)
}