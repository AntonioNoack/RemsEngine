package me.anno.image.svg

fun svgTokenize(str: String): ArrayList<Any> {
    val tokens = ArrayList<Any>()
    var i = 0
    loop@ while (i < str.length) {
        when (val char = str[i]) {
            ' ', '\t', '\n', '\r', '+' -> i++ // idc
            '(', ')', ',' -> {
                tokens += char
                i++
            }
            in '0'..'9', '-' -> { // read number
                if (char == '-') i++
                val i0 = i
                digits@ while (i < str.length) {
                    when (str[i]) {
                        in '0'..'9' -> i++
                        else -> break@digits
                    }
                }
                if (i < str.length && str[i] == '.') {
                    i++
                    digits@ while (i < str.length) {
                        when (str[i]) {
                            in '0'..'9' -> i++
                            else -> break@digits
                        }
                    }
                }
                if (i + 1 < str.length && (str[i] == 'e' || str[i] == 'E')) {
                    i++
                    if (str[i] == '+' || str[i] == '-') i++
                    digits@ while (i < str.length) {
                        when (str[i]) {
                            in '0'..'9' -> i++
                            else -> break@digits
                        }
                    }
                }
                val ie = i
                if (i < str.length && when (str[i]) {
                        'f', 'F', 'd', 'D', 'l', 'L' -> true
                        else -> false
                    }
                ) i++
                tokens += str.substring(i0, ie).toDouble()
            }
            in 'A'..'Z', in 'a'..'z' -> { // read name
                val i0 = i
                while (i < str.length) {
                    when (str[i]) {
                        in 'A'..'Z', in 'a'..'z', '_', in '0'..'9' -> i++
                        else -> {
                            tokens.add(str.substring(i0, i))
                            continue@loop
                        }
                    }
                }
                tokens.add(str.substring(i0))
            }
            else -> throw RuntimeException("Unknown char $char in $str at position $i, $tokens")
        }
    }

    return tokens
}