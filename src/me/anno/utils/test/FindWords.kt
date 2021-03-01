package me.anno.utils.test

import kotlin.math.min

val minLength = 4

fun main() {
    // with a dictionary, it could find words automatically
    // so it can just display all substrings in the field nicely,
    // which is already a great help
    /*findWords(
        listOf(
            "012",
            "345",
            "678"
        )
    )*/
    /*findWords(
        listOf(
            "012a",
            "345b",
            "678c"
        )
    )*/
    /*findWords(
        listOf(
            "012",
            "345",
            "678",
            "abc",
            "def"
        )
    )*/
    // (homework of my (girl)friend)
    findWords(
        listOf(
            "mdyeersgdwtjgng",
            "bjrsmoedrnanrrr",
            "lluihifteyisyae",
            "layerssmugretye",
            "crqnacnsararyen",
            "zoeygozkicnbkph",
            "destructionaoko",
            "anoitaidarnlbnu",
            "oevppiiodwlmyls",
            "sncrdcliwuaujye",
            "eokoxhqjtceffec",
            "uzatuckikwitvli",
            "voqefgohyigknjh",
            "gjtcpndissolved",
            "psvteprdyjjolvj"
        )
    )
}

fun findWords(text: List<String>) {
    val lines = text
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val sizeX = lines[0].length
    val sizeY = lines.size
    // for all directions
    // 01 11 10
    // and reversed
    // check if it contains a word
    println(0)
    lines.forEach { checkSubWords(it) }
    println(1)
    for (i in 0 until sizeX) {
        checkSubWords(String(CharArray(sizeY) { lines[it][i] }))
    }
    println(2)
    checkCrossSections(lines, sizeX, sizeY)
    println(3)
    checkCrossSections(lines.reversed(), sizeX, sizeY) // to go left
}

fun checkCrossSections(lines: List<String>, sizeX: Int, sizeY: Int) {
    // all cross sections...
    for (startIndex in 0 until sizeX) {
        val length = min(sizeX - startIndex, sizeY)
        if (length >= minLength) {
            val words = String(CharArray(length) {
                lines[it][startIndex + it]
            })
            checkSubWords(words)
        }
    }
    for (startIndex in 1 until sizeY) {
        val length = min(sizeY - startIndex, sizeX)
        if (length >= minLength) {
            val words = String(CharArray(length) {
                lines[startIndex + it][it]
            })
            checkSubWords(words)
        }
    }
}

fun checkSubWords(total: String) {
    println("V $total")
    println("R ${total.reversed()}")
    /*if (total.length == minLength) checkWord(total)
    else if (total.length > minLength) {
        for (startIndex in 0 until total.length - minLength) {
            for (endIndex in startIndex + minLength until total.length) {
                val word = total.substring(startIndex, endIndex)
                if (word.length < minLength) throw RuntimeException()
                checkWord(word)
                checkWord(word.reversed())
            }
        }
    }*/
}

val processed = HashSet<String>()
fun checkWord(word: String) {
    if (word !in processed) {
        processed += word
        println(word)
    }
}