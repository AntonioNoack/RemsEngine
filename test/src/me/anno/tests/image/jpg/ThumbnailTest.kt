package me.anno.tests.image.jpg

import me.anno.image.jpg.JPGThumbnails.extractThumbnail
import me.anno.tests.LOGGER
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures

fun main() {
    // thread: https://stackoverflow.com/questions/10349622/extract-thumbnail-from-jpeg-file
    desktop.getChild("jpg").mkdirs()
    for (file in pictures.listChildren()!!) {
        if (file.isDirectory) continue
        if (file.lcExtension != "jpg") continue
        extractThumbnail(file) { data ->
            if (data != null) desktop.getChild("jpg/" + file.name).writeBytes(data)
            else LOGGER.debug("didn't find thumbs in $file")
        }
    }
}