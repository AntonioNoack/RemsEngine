package me.anno.io.zip

import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertEquals
import me.anno.utils.async.Callback
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

// read the contents of a .jar that was turned into a .exe
//  also, once we understand it, it would be nice to create .exe files ourselves from .jar files...
/**
 * .exe-files created from Launch4j are exe-files with the .jar file just appended at the end ->
 * parse the .exe-part, and read the contents from it :3
 *
 * documentation: https://learn.microsoft.com/en-us/windows/win32/debug/pe-format
 * */
object ExeSkipper {
    @JvmStatic
    fun main(args: Array<String>) {
        val src = desktop.getChild("RemsStudio 1.3.1.exe")
        desktop.getChild("Extracted.zip").writeBytes(getBytesAfterExeSections(src))
    }

    fun readAsFolder(file: FileReference, callback: Callback<InnerFolder>) {
        val bytes = getBytesAfterExeSections(file)
        InnerZipFile.createZipRegistryV2(file, bytes, callback)
    }

    fun getBytesAfterExeSections(src: FileReference): ByteArray {
        val bytes = src.readByteBufferSync(true)
            .order(ByteOrder.LITTLE_ENDIAN)
        skipExeFile(bytes)
        val endBytes = ByteArray(bytes.remaining())
        bytes.get(endBytes)
        return endBytes
    }

    fun skipExeFile(bytes: ByteBuffer): Int {
        bytes.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals('M'.code, bytes.get().toInt())
        assertEquals('Z'.code, bytes.get().toInt())
        bytes.position(0x3c)
        val signatureOffset = bytes.getInt()
        bytes.position(signatureOffset)
        assertEquals('P'.code, bytes.get().toInt())
        assertEquals('E'.code, bytes.get().toInt())
        assertEquals(0, bytes.get().toInt())
        assertEquals(0, bytes.get().toInt())
        val machineType = bytes.getShort() // 0x14c -> IMAGE_FILE_MACHINE_I386
        // println("MachineType: 0x${machineType.toString(16)}")
        val numSections = bytes.getShort()
        val timeDateStamp = bytes.getInt()
        // println("#Sections: $numSections")
        // println("Timestamp: ${Date(timeDateStamp * 1000L)}")
        val symbolTablePtr = bytes.getInt()
        val numSymbols = bytes.getInt()
        val optHeaderSize = bytes.getShort()
        // println("#Symbols: $numSymbols, starting at $symbolTablePtr")
        // println("opt-header-size: $optHeaderSize")
        val flags = bytes.getShort()
        // println("Flags: 0x${flags.toString(16)}")
        val headerStart = bytes.position()
        val optHeaderMagic = bytes.getShort().toInt()
        // val isPEPlus = optHeaderMagic == 0x20b
        assert(optHeaderMagic == 0x10b || optHeaderMagic == 0x20b) // PE32/PE32+
        /*// optional header standard fields
        val majorLinkerVersion = bytes.get()
        val minorLinkerVersion = bytes.get()
        val codeSize = bytes.getInt()
        val sumDataSectionsSize = bytes.getInt()
        val sumEmptySectionsSize = bytes.getInt()
        val entryPointPtr = bytes.getInt()
        val baseOfCode = bytes.getInt()
        val baseOfData = if (isPEPlus) bytes.getInt() else 0*/
        val sectionStride = 40
        var maxAddress = bytes.position()
        for (i in 0 until numSections) {
            val sectionPtr = headerStart + optHeaderSize + sectionStride * i
            bytes.position(sectionPtr)
            /*val name = ByteArray(9)
            bytes.get(name, 0, 8)
            val nameStr = String(name, 0, name.indexOf(0))
            val virtualSize = bytes.getInt()
            val virtualAddress = bytes.getInt()*/
            val rawDataSize = bytes.getInt(sectionPtr + 16)
            val rawDataPtr = bytes.getInt(sectionPtr + 20)
            maxAddress = max(maxAddress, rawDataPtr + rawDataSize)
            /*val relocationPtr = bytes.getInt()
            val lineNrPtr = bytes.getInt()
            val numRelocations = bytes.getShort()
            val numLineNrs = bytes.getShort()
            val flagsI = bytes.getInt()
            println("Section[$i].name: $nameStr, *$rawDataPtr += $rawDataSize, flags: ${flagsI.toString(16)}")*/
        }
        bytes.position(maxAddress)
        return maxAddress
    }
}