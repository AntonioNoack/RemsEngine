package me.anno.ui.editor.files.thumbs.test

import me.anno.ui.editor.files.thumbs.WindowsThumbnails
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.utils.OS
import java.io.EOFException
import java.io.File

fun main() {
    // print the top 100 96x96 images
    val folder = File(OS.home, "AppData\\Local\\Microsoft\\Windows\\Explorer")
    val size = 96
    val file = File(folder, "thumbcache_$size.db")
    var ctr = 0
    WindowsThumbnails.readDB(file, {
        if (ctr++ > 100) throw EOFException() // signal, that we are done
        true
    }) { fileName, index, data ->
        File(File(OS.desktop, "out"), fileName.toAllowedFilename() ?: "out$index").writeBytes(data)
    }
}
