package me.anno.io.windows

import me.anno.io.files.FileReference
import me.anno.utils.types.InputStreams.readNBytes2
import java.io.*
import java.nio.charset.StandardCharsets
import java.text.ParseException

/**
 * Represents a Windows shortcut (typically visible to Java only as a '.lnk' file).
 *
 * Retrieved 2011-09-23 from http://stackoverflow.com/questions/309495/windows-shortcut-lnk-parser-in-java/672775#672775
 * Originally called LnkParser
 *
 * Additional information on the format can be found at
 * https://web.archive.org/web/20190625054252/http://www.i2s-lab.com/Papers/The_Windows_Shortcut_File_Format.pdf
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
class WindowsShortcut {

    /**
     * Tests if the shortcut points to a directory.
     * @return true if the 'directory' bit is set in this shortcut, false otherwise
     */
    var isDirectory = false
        private set

    /**
     * Tests if the shortcut points to a local resource.
     * @return true if the 'local' bit is set in this shortcut, false otherwise
     */
    var isLocal = false
        private set

    /**
     * @return the name of the filesystem object pointed to by this shortcut
     */
    var absolutePath: String? = null
        private set

    /**
     * @return a description for this shortcut, or null if no description is set
     */
    var description: String? = null
        private set

    /**
     * @return the relative path for the filesystem object pointed to by this shortcut, or null if no relative path is set
     */
    var relativePath: String? = null
        private set

    /**
     * @return the working directory in which the filesystem object pointed to by this shortcut should be executed, or null if no working directory is set
     */
    var workingDirectory: String? = null
        private set

    /**
     * @return the command line arguments that should be used when executing the filesystem object pointed to by this shortcut, or null if no command line arguments are present
     */
    var commandLineArguments: String? = null
        private set

    @Suppress("unused")
    constructor()

    constructor(file: FileReference) {
        file.inputStream().use { input ->
            parseLink(input.readNBytes2(maxLength, false))
        }
    }

    // todo make this use InputStreams instead of buffers

    /**
     * Gobbles up link data by parsing it and storing info in member fields
     *
     * @param link all the bytes from the .lnk file
     */
    @Throws(ParseException::class)
    private fun parseLink(link: ByteArray) {
        try {
            if (!isMagicPresent(link)) throw ParseException("Invalid shortcut; magic is missing", 0)

            // get the flags byte
            val flags = link[0x14].toInt()

            // get the file attributes byte
            val fileAttributes = link[0x18].toInt()
            isDirectory = fileAttributes and 0x10 > 0

            // if the shell settings are present, skip them
            var shellLen = 0
            if (flags and 1 > 0) {
                // the plus 2 accounts for the length marker itself
                shellLen = readLE16(link, 0x4c) + 2
            }

            // get to the file settings
            val fileStart = 0x4c + shellLen
            val fileLocationInfoFlagOffsetOffset = 0x08
            val fileLocationInfoFlag = link[fileStart + fileLocationInfoFlagOffsetOffset].toInt()
            isLocal = fileLocationInfoFlag and 1 == 1
            // get the local volume and local system values
            val finalNameOffset = readLE32(link, fileStart + 0x18) + fileStart
            val finalName = getNullDelimitedString(link, finalNameOffset)
            absolutePath = if (isLocal) {
                val basenameOffset = readLE32(link, fileStart + 0x10) + fileStart
                val basename = getNullDelimitedString(link, basenameOffset)
                basename + finalName
            } else {
                val networkVolumeTableOffset = link[fileStart + 0x14] + fileStart
                val shareNameOffsetOffset = 0x08
                val shareNameOffset = link[networkVolumeTableOffset + shareNameOffsetOffset] + networkVolumeTableOffset
                val shareName = getNullDelimitedString(link, shareNameOffset)
                "$shareName/$finalName"
            }

            // parse additional strings coming after file location
            val fileLocationSize = readLE32(link, fileStart)
            var nextStringStart = fileStart + fileLocationSize
            val hasDescription = 4
            val hasRelativePath = 8
            val hasWorkingDirectory = 16
            val hasCommandLineArguments = 32

            // if description is present, parse it
            if (flags and hasDescription > 0) {
                val stringLen = readLE16(link, nextStringStart) * 2 // times 2 because UTF-16
                description = readUTF16LE(link, nextStringStart + 2, stringLen)
                nextStringStart += stringLen + 2
            }

            // if relative path is present, parse it
            if (flags and hasRelativePath > 0) {
                val stringLen = readLE16(link, nextStringStart) * 2 // times 2 because UTF-16
                relativePath = readUTF16LE(link, nextStringStart + 2, stringLen)
                nextStringStart += stringLen + 2
            }

            // if working directory is present, parse it
            if (flags and hasWorkingDirectory > 0) {
                val stringLen = readLE16(link, nextStringStart) * 2 // times 2 because UTF-16
                workingDirectory = readUTF16LE(link, nextStringStart + 2, stringLen)
                nextStringStart += stringLen + 2
            }

            // if command line arguments are present, parse them
            if (flags and hasCommandLineArguments > 0) {
                val stringLen = readLE16(link, nextStringStart) * 2 // times 2 because UTF-16
                commandLineArguments = readUTF16LE(link, nextStringStart + 2, stringLen)
                // next_string_start = next_string_start + string_len + 2;
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw ParseException("Could not be parsed, probably not a valid WindowsShortcut", 0)
        }
    }

    companion object {

        /**
         * an arbitrary limit, that hopefully never is passed;
         * prevents loading gigabytes for corrupted link files
         * */
        private const val maxLength = 1 shl 16

        /**
         * Provides a quick test to see if this could be a valid link
         * If you try to instantiate a new WindowShortcut and the link is not valid,
         * Exceptions may be thrown and Exceptions are extremely slow to generate,
         * therefore any code needing to loop through several files should first check this.
         *
         * @param file the potential link
         * @return true if it may be a link, false otherwise
         * @throws IOException if an IOException is thrown while reading from the file
         */
        @Throws(IOException::class)
        fun isPotentialValidLink(file: FileReference): Boolean {
            val minimumLength = 0x64
            if (file.lcExtension != "lnk" || file.isDirectory || file.length() < minimumLength) return false
            file.inputStream().use { fis -> return isMagicPresent(fis.readNBytes2(32, false)) }
        }

        private fun isMagicPresent(link: ByteArray): Boolean {
            return link.size >= 32 && readLE32(link, 0) == 0x4C
        }

        private fun getNullDelimitedString(bytes: ByteArray, start: Int): String {
            // count bytes until the null character (0)
            var index = start
            while (index < bytes.size && bytes[index] != 0.toByte()) {
                index++
            }
            return String(bytes, start, index - start)
        }

        private fun readUTF16LE(bytes: ByteArray, off: Int, len: Int) =
            String(bytes, off, len, StandardCharsets.UTF_16LE)

        /**
         * convert two bytes into a short note, this is little endian because it's for an Intel only OS.
         */
        private fun readLE16(bytes: ByteArray, off: Int) =
            ((bytes[off + 1].toInt() and 0xff) shl 8) or (bytes[off].toInt() and 0xff)

        private fun readLE32(bytes: ByteArray, off: Int) =
            readLE16(bytes, off + 2) shl 16 or readLE16(bytes, off)

    }
}