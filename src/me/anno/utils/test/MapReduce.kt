package me.anno.utils.test

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.Clock
import me.anno.utils.LOGGER
import me.anno.utils.OS.downloads

fun main() {

    // this sample used 16s in Python:
    // https://www.michael-noll.com/tutorials/writing-an-hadoop-mapreduce-program-in-python/
    // and I wondered, what decently efficient performance by doing things right would look like
    // result: 0.25s io + 0.25s computation in Kotlin

    // on the ARA cluster with Hadoop, it uses 25s without combination stage, and 45s with.
    // because of this, I first had issues understanding the point of our course Big Data
    // now we're doing sensible and interesting things :)

    val clock = Clock()

    val folder = getReference(downloads, "WordCount.zip/WordCount_v1")

    clock.stop("reading zip")

    val books = folder.listChildren()!!
        .filter { it.lcExtension == "txt" }
        .map { file ->
            file.readText()
                .split(' ')
                .map { word -> word.trim() }
                .filter { it.isNotEmpty() }
        }

    val total = HashMap<String, Int>()
    for (book in books) {
        for (word in book) {
            total[word] = (total[word] ?: 0) + 1
        }
    }

    for ((key, value) in total.entries.sortedBy { -it.value }.subList(0, 10)) {
        LOGGER.info("$key: ${value}x")
    }

    clock.stop("word counting")

}