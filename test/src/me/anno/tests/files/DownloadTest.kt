package me.anno.tests.files

import me.anno.Time
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.utils.OS.downloads
import me.anno.utils.files.Files.formatFileSize

// this is fast enough :3, -> so use this for downloads
fun main() {
    val src = getReference(
        "https://www.dropbox.com/scl/fi/bhst74qy43sf02z00wh58/" +
                "allCompact.jar?rlkey=d9yvjkrudxj2cjup8kcg0ltyj&dl=1"
    )
    val dst = downloads.getChild("lib/spellchecking/allCompact.dropbox.jar")
    val t0 = Time.nanoTime
    var lastUpdate = 0L
    var lastTotal = 0L
    src.copyTo(dst, { _, total ->
        val t1 = Time.nanoTime
        if (t1 - lastUpdate >= SECONDS_TO_NANOS) {
            val avgRate = total * SECONDS_TO_NANOS / (t1 - t0) // B/s
            val currRate = (total - lastTotal) * SECONDS_TO_NANOS / (t1 - lastUpdate)
            println("Rate: ${currRate.formatFileSize()}/s, avg: ${avgRate.formatFileSize()}/s, total: ${total.formatFileSize()}")
            lastUpdate = t1
            lastTotal = total
        }
    }) {}
}