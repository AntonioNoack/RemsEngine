package me.anno.tests.utils.terminal

import me.anno.config.DefaultConfig
import me.anno.ui.base.text.TextPanel
import me.anno.ui.debug.TestEngine.Companion.testUI
import org.apache.logging.log4j.PrintColor

fun main() {
    // test whether drawing text properly consumes terminal-style-sequences
    val text = PrintColor.run {
        // todo underline is not shown, why???
        val gold = 0xffc900
        style(
            "This is an " + style("error", UNDERLINE, color(gold)) + " message ",
            RED, BOLD
        ) + style("Strikethrough!", STRIKETHROUGH) + " \n" +
                style("Underline", UNDERLINE)
    }
    testUI("Terminal Style", TextPanel(text, DefaultConfig.style))
}