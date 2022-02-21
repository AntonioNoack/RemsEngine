package me.anno.utils.test.structures

import me.anno.utils.Clock.Companion.measure
import java.util.*

fun main() {

    val alphabet = "abcdefghijklmnopqrstuvxyz0123456"
    val random = Random(1234)
    val randomStrings = Array(1024) {
        String(CharArray(1024) { alphabet[random.nextInt(alphabet.length)] })
    }

    val turns = 2048

    // 1.32s
    measure("toCharArray0") {
        var sum = 0
        for (str in randomStrings) {
            for (i in 0 until turns) {
                sum += str.toCharArray().sumOf { it.code }
            }
        }
    }

    // 1.63s
    measure("toCharArray1") {
        var sum = 0
        for (str in randomStrings) {
            for (i in 0 until turns) {
                val chars = str.toCharArray()
                for (c in chars) {
                    sum += c.code
                }
            }
        }
    }

    // 1.22s
    measure("iterator()") {
        var sum = 0
        for (str in randomStrings) {
            for (i in 0 until turns) {
                for (c in str) {
                    sum += c.code
                }
            }
        }
    }

    // 0.59s -> use this
    measure("getCharAt0") {
        var sum = 0
        for (str in randomStrings) {
            for (i in 0 until turns) {
                for (j in str.indices) {
                    sum += str[j].code
                }
            }
        }
    }

    // 0.59s -> use the one above
    measure("getCharAt1") {
        var sum = 0
        for (str in randomStrings) {
            for (i in 0 until turns) {
                for (j in 0 until str.length) {
                    sum += str[j].code
                }
            }
        }
    }

    // 6.2s -> don't use
    measure("codePoints()") {
        var sum = 0
        for (str in randomStrings) {
            for (i in 0 until turns) {
                sum += str.codePoints().sum()
            }
        }
    }

}