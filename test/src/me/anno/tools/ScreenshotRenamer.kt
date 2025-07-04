package me.anno.tools

import me.anno.io.files.FileReference
import me.anno.utils.OS.screenshots
import org.apache.logging.log4j.LogManager
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Windows just names screenshots "Screenshot (<number>)" instead of something like the date.
 * This is stupid, so let's correct it (until it's too late, and I lose their metadata).
 * */
val badWindowsFormat = Regex("Screenshot \\(\\d+\\)\\.png")
val badDateFormat = Regex("\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d-\\d\\d\\.\\d\\d\\.\\d\\d\\.png")
val goodFormat = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")

private val logger = LogManager.getLogger("ScreenshotRenaming")

fun main() {
    val map = HashMap<String,Int>(256)
    rename1(screenshots, map)
    logger.info("Renamed ${map.size} files")
}

fun rename1(file: FileReference, dates: HashMap<String, Int>) {
    if (file.isDirectory) {
        for (child in file.listChildren()) {
            rename1(child, dates)
        }
    } else if (badWindowsFormat.matches(file.name) || badDateFormat.matches(file.name)) {
        val newName = goodFormat.format(Date(file.lastModified))
        val id = dates[newName] ?: 0
        dates[newName] = id + 1
        val newName2 = if (id > 0) "$newName-$id.png" else "$newName.png"
        file.renameTo(file.getSibling(newName2))
    } else logger.info("Ignored $file")
}