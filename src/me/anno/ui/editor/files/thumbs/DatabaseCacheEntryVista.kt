package me.anno.ui.editor.files.thumbs

/**
 * vista
 * */
open class DatabaseCacheEntryVista(
    magicIdentifier: Magic,
    cacheEntrySize: Int,
    entryHash: Long,
    val extension: WideChar4,
    filenameLength: Int,
    paddingSize: Int,
    dataSize: Int,
    unknown: Int,
    dataChecksum: Long,
    headerChecksum: Long
) : DatabaseCacheEntry7(
    magicIdentifier,
    cacheEntrySize,
    entryHash,
    filenameLength,
    paddingSize,
    dataSize,
    unknown,
    dataChecksum,
    headerChecksum
) {
    constructor(input: WindowsThumbDBStream) : this(
        input.readMagic(), input.readInt(), input.readLong(), input.readWideChar4(), input.readInt(),
        input.readInt(), input.readInt(), input.readInt(), input.readLong(), input.readLong()
    )
}