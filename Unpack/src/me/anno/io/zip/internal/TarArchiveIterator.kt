package me.anno.io.zip.internal

import me.anno.utils.structures.NextEntryIterator
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream

class TarArchiveIterator(val file: ArchiveInputStream) : NextEntryIterator<ArchiveEntry>() {
    override fun nextEntry(): ArchiveEntry? = file.nextEntry
}