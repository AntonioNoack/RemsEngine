package me.anno.ui.editor.files.thumbs.test

import me.anno.ui.editor.files.thumbs.WindowsThumbnails
import me.anno.ui.editor.files.toAllowedFilename
import me.anno.utils.OS
import net.boeckling.crc.CRC64
import java.io.EOFException
import java.io.File

fun Long.toUString() = toULong().toString(16)

fun calculateHashKey(seed0: ULong, buffer: ByteArray, buffer_length: Int): ULong {
    var seed = seed0
    for(count in 0 until buffer_length){
        seed = seed xor ((seed shr 2) + (2080u * seed) + buffer[count].toUInt())
    }
    return seed
}

fun getThumbnailCacheId(
    VolGUID: ByteArray,
    FileID: ByteArray,
    FileExtension: ByteArray,
    FileModTime: ByteArray
): ULong {
    var hash = calculateHashKey(0x95E729BA2C37FD21u, VolGUID, 16)
    hash = calculateHashKey(hash, FileID, 8)
    hash = calculateHashKey(hash, FileExtension, FileExtension.size * 2)
    hash = calculateHashKey(hash, FileModTime, 4)
    return hash
}

fun main() {

    // generating the correct hash sadly seems to be very complicated...
    // as we need all these inputs...

    val folder = File("C:\\Users\\Antonio\\Pictures\\YandereSimulator")
    val cases = listOf(
        0x2eeb8611ad398d24 to File(folder, "Photo_18.png"),
        0x3bc7d77f07290159 to File(folder, "Photo_7.png")
    )

    cases.forEach { (crc, file) ->
        val crc2 = CRC64.fromInputStream(file.inputStream()).value
        println(CRC64.fromInputStream(file.toString().byteInputStream()))
        println("${crc.toUString()}: ${crc2.toUString()}")
    }

    return

    // print the top 100 96x96 images
    val folder2 = File(OS.home, "AppData\\Local\\Microsoft\\Windows\\Explorer")
    val size = 96
    val file = File(folder2, "thumbcache_$size.db")
    var ctr = 0
    WindowsThumbnails.readDB(file, {
        if (ctr++ > 100) throw EOFException() // signal, that we are done
        true
    }) { fileName, index, data ->
        File(File(OS.desktop, "out"), fileName.toAllowedFilename() ?: "out$index").writeBytes(data)
    }
}
