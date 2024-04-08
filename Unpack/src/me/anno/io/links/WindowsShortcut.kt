package me.anno.io.links

import me.anno.io.Streams.readNBytes2
import me.anno.io.files.FileReference
import me.anno.utils.structures.Callback
import me.anno.utils.types.Booleans.hasFlag
import java.io.IOException
import java.text.ParseException

/**
 * Represents a Windows shortcut (typically visible to Java only as a '.lnk' file).
 * Official file format documentation: https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-shllink/16cb4ca1-9339-4d0c-a68d-bf1d6cc0f943
 *
 * Retrieved 2011-09-23 from http://stackoverflow.com/questions/309495/windows-shortcut-lnk-parser-in-java/672775#672775
 * Originally called LnkParser
 *
 * Additional information on the format can be found at
 * https://web.archive.org/web/20070208051731/http://www.i2s-lab.com/Papers/The_Windows_Shortcut_File_Format.pdf
 *
 * Written by: (the stack overflow users, obviously!)
 * Dword fix for offsets within file location structure  by file extension by JS Lair https://stackoverflow.com/users/10297367/js-lair https://github.com/JSLair
 * Filtering potential links by file extension by JS Lair https://stackoverflow.com/users/10297367/js-lair https://github.com/JSLair
 * "isLocal" bit fix by Naxos84 https://stackoverflow.com/users/3157899/naxos84 https://github.com/Naxos84
 * Apache Commons VFS dependency removed by crysxd (why were we using that!?) https://github.com/crysxd
 * Headerified, refactored and commented by Code Bling http://stackoverflow.com/users/675721/code-bling
 * Network file support added by Stefan Cordes http://stackoverflow.com/users/81330/stefan-cordes
 * Adapted by Sam Brightman http://stackoverflow.com/users/2492/sam-brightman
 * Support for additional strings (description, relative_path, working_directory, command_line_arguments) added by Max Vollmer https://stackoverflow.com/users/9199167/max-vollmer
 * Based on information in 'The Windows Shortcut File Format' by Jesse Hager <jessehager@iname.com>
 * And somewhat based on code from the book 'Swing Hacks: Tips and Tools for Killer GUIs' by Joshua Marinacci and Chris Adamson
 * ISBN: 0-596-00907-0
 * http://www.oreilly.com/catalog/swinghks/
 */
@Suppress("SpellCheckingInspection")
class WindowsShortcut(data: ByteArray) {

    var isDirectory = false
        private set

    var isLocalResource = false
        private set

    var absolutePath: String? = null
        private set

    var description: String? = null
        private set

    var relativePath: String? = null
        private set

    /**
     * for execution
     */
    var workingDirectory: String? = null
        private set

    /**
     * for execution
     */
    var commandLineArguments: String? = null
        private set

    var iconPath: String? = null
        private set

    init {
        try {
            if (!isMagicPresent(data)) throw IOException("Invalid shortcut; magic is missing")

            // get the flags byte
            val flags = data[0x14].toInt()

            // get the file attributes byte
            val fileAttributes = data[0x18].toInt()
            isDirectory = fileAttributes.hasFlag(0x10)

            // if the shell settings are present, skip them
            var shellLen = 0
            if (flags.hasFlag(1)) {
                // the plus 2 accounts for the length marker itself
                shellLen = readLE16(data, 0x4c) + 2
            }

            // get to the file settings
            val fileStart = 0x4c + shellLen
            // get the local volume and local system values
            val fileLocationSize = readLE32(data, fileStart)
            val fileLocationInfoFlag = data[fileStart + 0x08].toInt()
            val basenameOffset = readLE32(data, fileStart + 0x10) + fileStart
            val networkVolumeTableOffset = data[fileStart + 0x14] + fileStart
            val finalNameOffset = readLE32(data, fileStart + 0x18) + fileStart
            val finalName = getNullDelimitedString(data, finalNameOffset)
            isLocalResource = fileLocationInfoFlag.hasFlag(1)
            absolutePath = if (isLocalResource) {
                val basename = getNullDelimitedString(data, basenameOffset)
                basename + finalName
            } else {
                val shareNameOffset = data[networkVolumeTableOffset + 0x08] + networkVolumeTableOffset
                val shareName = getNullDelimitedString(data, shareNameOffset)
                "$shareName/$finalName"
            }

            // parse additional strings coming after file location
            var nextStringStart = fileStart + fileLocationSize

            // if description is present, parse it
            if (flags.hasFlag(4)) {
                val stringLen = readLE16(data, nextStringStart) shl 1 // times 2 because UTF-16
                description = readUTF16LE(data, nextStringStart + 2, stringLen)
                nextStringStart += stringLen + 2
            }

            // if relative path is present, parse it
            if (flags.hasFlag(8)) {
                val stringLen = readLE16(data, nextStringStart) shl 1 // times 2 because UTF-16
                relativePath = readUTF16LE(data, nextStringStart + 2, stringLen)
                nextStringStart += stringLen + 2
            }

            // if working directory is present, parse it
            if (flags.hasFlag(16)) {
                val stringLen = readLE16(data, nextStringStart) shl 1 // times 2 because UTF-16
                workingDirectory = readUTF16LE(data, nextStringStart + 2, stringLen)
                nextStringStart += stringLen + 2
            }

            // if command line arguments are present, parse them
            if (flags.hasFlag(32)) {
                val stringLen = readLE16(data, nextStringStart) shl 1 // times 2 because UTF-16
                commandLineArguments = readUTF16LE(data, nextStringStart + 2, stringLen)
                nextStringStart += stringLen + 2
            }

            // if file has link, retrieve it
            if (flags.hasFlag(64)) {
                val stringLen = readLE16(data, nextStringStart) shl 1 // times 2 because UTF-16
                iconPath = readUTF16LE(data, nextStringStart + 2, stringLen)
                nextStringStart += stringLen + 2
            }
        } catch (e: IndexOutOfBoundsException) {
            throw ParseException("Could not be parsed, probably not a valid WindowsShortcut", 0)
        }
    }

    companion object {

        /**
         * an arbitrary limit, that hopefully never is passed;
         * prevents loading gigabytes for corrupted link files
         * */
        private const val maxLength = 1 shl 16

        fun get(file: FileReference, callback: Callback<WindowsShortcut>) {
            file.inputStream { it, exc ->
                if (it != null) {
                    val data = it.readNBytes2(maxLength, false)
                    callback.ok(WindowsShortcut(data))
                } else callback.err(exc)
            }
        }

        @JvmStatic
        private fun isMagicPresent(link: ByteArray): Boolean {
            return link.size >= 32 && readLE32(link, 0) == 0x4C
        }

        @JvmStatic
        private fun getNullDelimitedString(bytes: ByteArray, start: Int): String {
            // count bytes until the null character (0)
            var end = start
            while (end < bytes.size && bytes[end] != 0.toByte()) {
                end++
            }
            return bytes.decodeToString(start, end)
        }

        @JvmStatic
        private fun readUTF16LE(bytes: ByteArray, off: Int, byteLen: Int): String {
            return CharArray(byteLen / 2) {
                readLE16(bytes, off + it * 2).toChar()
            }.concatToString()
        }

        /**
         * convert two bytes into a short note, this is little endian because it's for an Intel only OS.
         */
        @JvmStatic
        private fun readLE16(bytes: ByteArray, off: Int) =
            ((bytes[off + 1].toInt() and 0xff) shl 8) or (bytes[off].toInt() and 0xff)

        @JvmStatic
        private fun readLE32(bytes: ByteArray, off: Int) =
            readLE16(bytes, off + 2).shl(16) or readLE16(bytes, off)
    }
}