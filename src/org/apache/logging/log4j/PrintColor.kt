package org.apache.logging.log4j

import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r

/**
 * different colors and styles that can be applied to printing in the terminal
 * not yet supported for rendering text -> todo support this somehow...
 *
 * todo when printing a file name in the logs, underline it;
 * todo when clicking on a log message, check if it is underlined, if so extract that section,
 *   remove all styles, and then try to open the link
 * */
object PrintColor {
    const val ESC = "\u001B["

    const val RESET = "${ESC}0m"

    // Styles
    const val BOLD = "${ESC}1m"
    const val THIN = "${ESC}2m"
    const val THIN_BOLD_OFF = "${ESC}22m"
    const val ITALIC = "${ESC}3m"
    const val ITALIC_OFF = "${ESC}23m"
    const val UNDERLINE = "${ESC}4m"
    const val UNDERLINE_OFF = "${ESC}24m"
    const val BLINK = "${ESC}5m"
    const val BLINK_OFF = "${ESC}25m"

    /**
     * changes text and background color
     * */
    const val REVERSE = "${ESC}7m"
    const val REVERSE_OFF = "${ESC}27m"
    const val HIDDEN = "${ESC}8m"
    const val HIDDEN_OFF = "${ESC}28m"
    const val STRIKETHROUGH = "${ESC}9m"
    const val STRIKETHROUGH_OFF = "${ESC}29m"

    // Foreground colors
    const val BLACK = "${ESC}30m"
    const val RED = "${ESC}31m"
    const val GREEN = "${ESC}32m"
    const val YELLOW = "${ESC}33m"
    const val BLUE = "${ESC}34m"
    const val MAGENTA = "${ESC}35m"
    const val CYAN = "${ESC}36m"
    const val WHITE = "${ESC}37m"

    // Bright foreground colors
    const val BRIGHT_BLACK = "${ESC}90m"
    const val BRIGHT_RED = "${ESC}91m"
    const val BRIGHT_GREEN = "${ESC}92m"
    const val BRIGHT_YELLOW = "${ESC}93m"
    const val BRIGHT_BLUE = "${ESC}94m"
    const val BRIGHT_MAGENTA = "${ESC}95m"
    const val BRIGHT_CYAN = "${ESC}96m"
    const val BRIGHT_WHITE = "${ESC}97m"

    // Background colors
    const val BG_BLACK = "${ESC}40m"
    const val BG_RED = "${ESC}41m"
    const val BG_GREEN = "${ESC}42m"
    const val BG_YELLOW = "${ESC}43m"
    const val BG_BLUE = "${ESC}44m"
    const val BG_MAGENTA = "${ESC}45m"
    const val BG_CYAN = "${ESC}46m"
    const val BG_WHITE = "${ESC}47m"

    // Bright background colors
    const val BG_BRIGHT_BLACK = "${ESC}100m"
    const val BG_BRIGHT_RED = "${ESC}101m"
    const val BG_BRIGHT_GREEN = "${ESC}102m"
    const val BG_BRIGHT_YELLOW = "${ESC}103m"
    const val BG_BRIGHT_BLUE = "${ESC}104m"
    const val BG_BRIGHT_MAGENTA = "${ESC}105m"
    const val BG_BRIGHT_CYAN = "${ESC}106m"
    const val BG_BRIGHT_WHITE = "${ESC}107m"

    fun color(color: Int) = color(color.r(), color.g(), color.b())
    fun color(r: Int, g: Int, b: Int) = "${ESC}38;2;$r;$g;${b}m"

    fun bgColor(color: Int) = bgColor(color.r(), color.g(), color.b())
    fun bgColor(r: Int, g: Int, b: Int) = "${ESC}48;2;$r;$g;${b}m"

    fun style(text: CharSequence, vararg codes: String): String {
        return style(text.toString(), codes.joinToString(""))
    }

    fun style(text: String, vararg codes: String): String {
        if (codes.isEmpty()) return text
        return style(text, codes.joinToString(""))
    }

    fun style(text: CharSequence, code: String): String {
        return style(text.toString(), code)
    }

    fun style(text: String, code: String): String {
        if (code.isEmpty()) return text

        return buildString {
            append(code)
            if (RESET in text) {
                // fix nested styles
                append(text.replace(RESET, RESET + code))
            } else append(text)
            append(RESET)
        }
    }

    private val removeStyleRegex = Regex("\\u001B\\[[0-9;]*m")
    fun removeStyles(text: String): String {
        return text.replace(removeStyleRegex, "")
    }

}