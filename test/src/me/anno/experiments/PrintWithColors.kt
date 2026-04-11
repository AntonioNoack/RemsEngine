package me.anno.experiments

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.PrintColor

fun main() {
    PrintColor.apply {
        // test some simple messages
        println(style("This is normal output", GREEN, UNDERLINE))
        println(style("This is an error message", RED, BOLD))
        println(style("This is a warning", YELLOW, ITALIC))

        // test some special effects/colors
        val gold = 0xffc900
        println(style("Golden Text", color(gold), BG_BLACK))
        println(style("Golden Background", bgColor(gold), BLACK))
        println(style("Hidden Text?", HIDDEN))
        println(style("Dim Text?", THIN))

        // test nested styles
        println( // color is kept, but I like it like that :)
            style(
                "This is an " + style("error", UNDERLINE) + " message",
                RED, BOLD
            )
        )
        println(
            style(
                "This is an " + style("error", UNDERLINE, color(gold)) + " message",
                RED, BOLD
            )
        )
    }

    val logger = LogManager.getLogger("PrintWithColors")
    logger.enable(Level.TRACE)
    logger.trace("Some trace")
    logger.debug("Some debug")
    logger.info("Some info")
    logger.warn("Some warning")
    logger.error("Some error")
    logger.severe("Something severe")
    logger.fatal("Something fatal")

}
