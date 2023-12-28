package me.anno.tests.mesh.gtav

import me.anno.io.Streams.readLE32
import me.anno.io.files.FileReference.Companion.getReference
import java.io.InputStream

// to do extract GTA V's meshes from its game files ->
//  the files are AES256 encrypted :/, and XCompress/ZLib compressed
// https://gtamods.com/wiki/RPF_archive
fun main() {
    val folder = getReference("G:\\Programs\\Epic\\GTAV")
    val file = folder.getChild("x64a.rpf")
    val stream = file.inputStreamSync()
    val version = when (stream.readLE32()) {
        0x52504638 -> "RDD2"
        0x52504637 -> "GTAV"
        else -> throw NotImplementedError()
    }
    println(version)
    val tableOfContentsSize = stream.readLE32() // bytes
    val numEntries = stream.readLE32() // including root
    println(tableOfContentsSize)
    println(numEntries)
}

data class FileEntry(
    val nameOffset: Int,
    val isDirectory: Boolean,
    val dirFirstTocEntryIndex: Int,
    val dirNumFileEntries: Int,
    val fileOffset: Int,
    val fileSize: Int,
    val fileUncompressedSize: Int,
)

fun InputStream.readFileEntry(): FileEntry {
    val name = readLE32()
    val isDir = when (name.ushr(24)) {
        0x00 -> false
        0x80 -> true
        else -> throw IllegalStateException()
    }
    val nameOffset = name.and(0xffffff)
    val v0 = readLE32()
    val v1 = readLE32()
    val v2 = readLE32()
    if (isDir && v1 != v2) {
        throw IllegalStateException("Expected same")
    }
    return FileEntry(
        nameOffset, isDir,
        v0, v1, v0, v1, v2
    )
}