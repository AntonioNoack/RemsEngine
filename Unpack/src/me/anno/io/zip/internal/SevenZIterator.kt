package me.anno.io.zip.internal

import me.anno.utils.structures.NextEntryIterator
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile

class SevenZIterator(val file: SevenZFile) : NextEntryIterator<SevenZArchiveEntry>() {
    override fun nextEntry(): SevenZArchiveEntry? = file.nextEntry
}