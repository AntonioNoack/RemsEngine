package me.anno.language.spellcheck

import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.Sleep
import me.anno.video.ffmpeg.FFMPEGUtils.formatPercent
import org.apache.logging.log4j.LogManager
import kotlin.math.min

val minLength = 4

fun main() {
    // with a dictionary, it could find words automatically
    // so it can just display all substrings in the field nicely,
    // which is already a great help
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
    val logger = LogManager.getLogger("FindWords")
    val lines = text
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val sizeX = lines[0].length
    val sizeY = lines.size
    // for all directions
    // 01 11 10
    // and reversed
    // check if it contains a word
    logger.info(0)
    lines.forEach { checkSubWords(it) }
    logger.info(1)
    for (i in 0 until sizeX) {
        checkSubWords(String(CharArray(sizeY) { lines[it][i] }))
    }
    logger.info(2)
    checkCrossSections(lines, sizeX, sizeY)
    logger.info(3)
    checkCrossSections(lines.reversed(), sizeX, sizeY) // to go left
    waitForResults()
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
    val logger = LogManager.getLogger("FindWords")
    logger.info("V $total")
    logger.info("R ${total.reversed()}")
    if (total.length == minLength) checkWord(total)
    else if (total.length > minLength) {
        for (startIndex in 0 until total.length - minLength) {
            for (endIndex in startIndex + minLength until total.length) {
                val word = total.substring(startIndex, endIndex)
                if (word.length < minLength) throw RuntimeException()
                checkWord(word)
                checkWord(word.reversed())
            }
        }
    }
}

val processed = HashSet<String>()
fun checkWord(word: String) {
    if (word !in processed) {
        processed += word
    }
}

fun waitForResults(timeoutMillis: Long = 30_000) {
    val logger = LogManager.getLogger("FindWords")
    val done = HashSet<String>()
    var timeSinceLastResult = System.nanoTime()
    val timeoutNanos = timeoutMillis * MILLIS_TO_NANOS
    val processed = ArrayList(processed.sortedBy { it.length })
    val totalLength = processed.size
    logger.info("Checking $totalLength 'words'")
    while (processed.isNotEmpty()) {
        for (word in processed) {
            val list = Spellchecking.check(word, true, word)
            if (list != null) {
                // found answer :)
                done.add(word)
                if (list.isEmpty()) {
                    logger.info("$word, ${formatPercent(totalLength - processed.size, totalLength)}%")
                }
            }
        }
        if (done.isEmpty()) {
            val deltaTime = System.nanoTime() - timeSinceLastResult
            if (deltaTime > timeoutNanos) {
                logger.info("Reached timeout!")
                break
            }
            Sleep.sleepShortly(false)
        } else {
            processed.removeAll(done)
            done.clear()
            timeSinceLastResult = System.nanoTime()
        }
    }
}