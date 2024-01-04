package me.anno.image.svg

import org.apache.logging.log4j.LogManager

private val LOGGER = LogManager.getLogger("SVGPathReader")

fun interface Action1 {
    fun run(symbol: Char, v: Float)
}

fun interface Action2 {
    fun run(symbol: Char, x: Float, y: Float)
}

fun interface Action4 {
    fun run(symbol: Char, x0: Float, y0: Float, x1: Float, y1: Float)
}

fun interface Action6 {
    fun run(symbol: Char, x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float)
}

fun interface ArcAction {
    fun run(symbol: Char, rx: Float, ry: Float, rot: Float, largeAngle: Boolean, sweep: Boolean, x2: Float, y2: Float)
}

fun readSVGPath(
    data: String, action0: Runnable, action1: Action1, action2: Action2,
    action4: Action4, action6: Action6, action7: ArcAction,
) {

    var i = 0
    fun float(): Float {
        var j = i
        spaces@ while (true) {
            when (data[j]) {
                ' ', '\t', '\r', '\n', ',' -> j++
                else -> break@spaces
            }
        }
        i = j
        when (data[j]) {
            '+', '-' -> j++
        }
        when (data[j]) {
            '.' -> {
                // LOGGER.info("starts with .")
                j++
                int@ while (true) {
                    when (data.getOrNull(j)) {
                        in '0'..'9' -> j++
                        else -> break@int
                    }
                }
            }
            else -> {
                int@ while (true) {
                    when (data.getOrNull(j)) {
                        in '0'..'9' -> j++
                        else -> break@int
                    }
                }
                if (data.getOrNull(j) == '.') {
                    j++
                    int@ while (true) {
                        when (data.getOrNull(j)) {
                            in '0'..'9' -> j++
                            else -> break@int
                        }
                    }
                }
            }
        }

        when (data.getOrNull(j)) {
            'e', 'E' -> {
                j++
                when (data.getOrNull(j)) {
                    '+', '-' -> j++
                }
                int@ while (true) {
                    when (data.getOrNull(j)) {
                        in '0'..'9' -> j++
                        else -> break@int
                    }
                }
            }
        }
        // LOGGER.info("'${data.substring(i, j)}' + ${data.substring(j, j+10)}")
        val value = data.substring(i, j).toFloat()
        i = j
        return value
    }

    var lastAction = ' '
    fun parseAction(s: Char): Boolean {
        try {
            when (s) {
                ' ', '\t', '\r', '\n' -> return false
                'M', 'm', 'L', 'l', 'T', 't' -> action2.run(s, float(), float())
                'H', 'h', 'V', 'v' -> action1.run(s, float())
                'C', 'c' -> action6.run(s, float(), float(), float(), float(), float(), float())
                'S', 's', 'Q', 'q' -> action4.run(s, float(), float(), float(), float())
                'A', 'a' -> action7.run(s, float(), float(), float(), float() != 0f, float() != 0f, float(), float())
                'Z', 'z' -> action0.run()
                else -> {
                    i--
                    parseAction(lastAction)
                    return false
                }
            }
        } catch (e: Exception) {
            LOGGER.info(data)
            throw e
        }
        return true
    }

    while (i < data.length) {
        when (val symbol = data[i++]) {
            ' ', '\t', '\r', '\n' -> {
            }
            else -> {
                if (parseAction(symbol)) {
                    lastAction = symbol
                }
            }
        }
    }
}