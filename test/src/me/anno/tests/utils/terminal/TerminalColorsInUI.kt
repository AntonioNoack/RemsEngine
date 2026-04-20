package me.anno.tests.utils.terminal

import me.anno.config.DefaultConfig
import me.anno.fonts.GlyphStyle.UNDERLINE_CHAR
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.StringStyles

fun main() {
    // test whether drawing text properly consumes terminal-style-sequences
    val text = StringStyles.run {
        val gold = 0xffc900
        // why is underline and strike-through white?? -> it was using the emoji-mode, because alpha = 1 :D
        "\n".repeat(5) + style(
            "This is an " + style("error", UNDERLINE, color(gold)) + " message ",
            RED, BOLD
        ) + style("Strike through!", STRIKETHROUGH) + " \n" +
                style("Underline", UNDERLINE) + " \n" +
                "Special Underline".map { "$it${UNDERLINE_CHAR}" }.joinToString("") +
                " Normal Text"
    }
    testUI("Terminal Style", TextPanel(text, DefaultConfig.style))
}