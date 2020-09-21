package me.anno.ui.editor.files.thumbs

import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.vista
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win10
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win7
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win8
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win8_1
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win8v2
import me.anno.ui.editor.files.thumbs.WindowsThumbDBVersions.win8v3
import me.anno.utils.readNBytes2
import java.io.EOFException
import java.io.File

object WindowsThumbnails {

    val dbMagic = magic("CMMM")
    fun magic(str: String) = str[0].toInt() + str[1].toInt().shl(8) + str[2].toInt().shl(16) + str[3].toInt().shl(24)

    fun readDB(
        file: File,
        isInterestedInFile: (crc64: Long) -> Boolean,
        callback: (fileName: String, index: Int, data: ByteArray) -> Unit
    ) {

        // a special input stream, which counts its position
        // and allows to jump back after marking a position
        val input = WindowsThumbDBStream(file.inputStream().buffered())
        val header = DatabaseHeader(input)
        if (header.magic != dbMagic) throw RuntimeException()

        // val winVersion = version[header.version] ?: throw RuntimeException("Unknown windows version: ${header.version}")
        // val size = sizesMap.getValue(header.version)[header.type] ?: "Unknown"
        // ("$winVersion, $size")

        /*var firstCacheEntry = 0
        var availableCacheEntry = 0
        var numberOfCacheEntries = 0*/

        when (header.version) {
            win8v2 -> {
                input.skip(16)
                /*val info = DatabaseHeaderEntryInfoV2(input)
                firstCacheEntry = info.firstCacheEntry
                availableCacheEntry = info.availableCacheEntry
                numberOfCacheEntries = info.numberOfCacheEntries*/
            }
            win8v3, win8_1, win10 -> {
                input.skip(12)
                /*val info = DatabaseHeaderEntryInfoV3(input)
                firstCacheEntry = info.firstCacheEntry
                availableCacheEntry = info.availableCacheEntry*/
            }
            else -> {
                input.skip(16)
                /*val info = DatabaseHeaderEntryInfo(input)
                firstCacheEntry = info.firstCacheEntry
                availableCacheEntry = info.availableCacheEntry
                numberOfCacheEntries = info.numberOfCacheEntries*/
            }
        }

        val entrySizeMax = 100
        var currentPosition = if (header.version == win8v2) 28 else 24
        input.mark(entrySizeMax)

        fun scanMemory(input: WindowsThumbDBStream): Boolean {
            var lastKey = 0
            val markDistance = 64
            input.mark(markDistance)
            while (true) {
                lastKey = lastKey.shr(8) + input.read().shl(24)
                if (lastKey == dbMagic) {
                    // found next entry point <3
                    input.jumpTo(input.ctr - 4)
                    return true
                }
                if (input.ctr and 31 == 0) {
                    input.mark(markDistance)
                }
            }
        }

        // Go through our database and attempt to extract each cache entry.
        try {
            var index = -1
            loop@ while (true) {
                index++

                // Set the file pointer to the end of the last cache entry.
                currentPosition = input.jumpTo(currentPosition)
                if (currentPosition < 0) break // eof reached
                input.mark(entrySizeMax)

                val entry: DatabaseCacheEntry7
                when (header.version) {
                    win7 -> {
                        entry = DatabaseCacheEntry7(input)
                        if (entry.magicIdentifier != dbMagic) {
                            input.jumpTo(currentPosition)
                            if (scanMemory(input)) {// valid entry was found
                                currentPosition = input.ctr
                                index--
                                continue@loop
                            }
                            break@loop // scan failed to find any more entries
                        }
                    }
                    vista -> {
                        entry = DatabaseCacheEntryVista(input)
                        if (entry.magicIdentifier != dbMagic) {
                            input.jumpTo(currentPosition)
                            if (scanMemory(input)) {// valid entry was found
                                currentPosition = input.ctr
                                index--
                                continue@loop
                            }
                            break@loop // scan failed to find any more entries
                        }
                    }
                    win8, win8v2, win8v3, win8_1, win10 -> {
                        entry = DatabaseCacheEntry8(input)
                        if (entry.magicIdentifier != dbMagic) {
                            input.jumpTo(currentPosition)
                            if (scanMemory(input)) {// valid entry was found
                                currentPosition = input.ctr
                                index--
                                continue@loop
                            }
                            break@loop // scan failed to find any more entries
                        }
                    }
                    else -> throw RuntimeException("not possible")
                }

                // I think this signifies the end of a valid database and everything beyond this is data that's been overwritten.
                if (entry.entryHash == 0L) {
                    // Skip the header of this entry. If the next position is invalid (which it probably will be), we'll end up scanning.
                    currentPosition = input.ctr
                    index--
                    continue@loop
                }

                // Cache size includes the 4 byte signature and itself ( 4 bytes ).
                currentPosition += entry.cacheEntrySize

                // dataChecksum: CRC-64

                // UTF-16 filename. Allocate the filename length
                // plus 6 for the unicode extension and null character (we don't need that in Java)
                var fileName = input.readNBytes2(entry.filenameLength).toCharArray()
                // entryHash = fileName
                // ("$fileName, ${entry.dataChecksum.toULong().toString(16)}, ${entry.entryHash.toULong().toString(16)}")

                if (isInterestedInFile(entry.entryHash)) {

                    // ("padding: ${entry.paddingSize}")
                    input.skip(entry.paddingSize.toLong())

                    val data = input.readNBytes2(entry.dataSize)
                    when {
                        data.startsWith(fileTypeBMP) -> {
                            fileName += ".bmp"
                        }
                        data.startsWith(fileTypeJPEG) -> {
                            fileName += ".jpg"
                        }
                        data.startsWith(fileTypePNG) -> {
                            fileName += ".png"
                        }
                        header.version == vista -> {
                            entry as DatabaseCacheEntryVista
                            fileName += entry.extension.toMagicString()
                        }
                    }

                    // do we need the file name?
                    callback(fileName, index, data)

                } else {

                    // not interested -> just skip it
                    input.skip(entry.paddingSize + entry.dataSize.toLong())

                }

                currentPosition = input.ctr

            }
        } catch (e: EOFException) {
            // done :)
        }

    }

    fun ByteArray.startsWith(list: List<Int>): Boolean {
        for ((index, entry) in list.withIndex()) {
            if (this[index].toInt().and(0xff) != entry) return false
        }
        return true
    }

    // fun ByteArray.toString2() = joinToString { it.toInt().byteToHex() }

    fun ByteArray.toCharArray(): String {
        val chars = CharArray(size / 2)
        var i = 0
        for (j in 0 until size / 2) {
            chars[j] = (this[i++].toInt().and(0xff) + this[i++].toInt().and(0xff).shl(8)).toChar()
        }
        return String(chars)
    }

    /*fun Int.toMagicString() = String(
        charArrayOf(
            this.and(0xff).toChar(),
            this.shr(8).and(0xff).toChar(),
            this.shr(16).and(0xff).toChar(),
            this.shr(24).and(0xff).toChar()
        )
    )*/

    fun Long.toMagicString() = String(
        charArrayOf(
            this.and(0xffff).toChar(),
            this.shr(16).and(0xffff).toChar(),
            this.shr(32).and(0xffff).toChar()// ,
            // this.shr(48).and(0xffff).toChar() 00 00
        )
    )

    val fileTypeBMP = listOf('B'.toInt(), 'M'.toInt())
    val fileTypeJPEG = listOf(0xff, 0xd8, 0xff, 0xe0)
    val fileTypePNG = listOf(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)


}