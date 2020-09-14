package me.anno.ui.editor.files.thumbs

/**
 * win 8
 * */
open class DatabaseCacheEntry8(
    magicIdentifier: Magic,
    cacheEntrySize: Int,
    entryHash: Long,
    filenameLength: Int,
    paddingSize: Int,
    dataSize: Int,
    val width: Int,
    val height: Int,
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
        input.readMagic(), input.readInt(), input.readLong(), input.readInt(),
        input.readInt(), input.readInt(), input.readInt(), input.readInt(),
        input.readInt(), input.readLong(), input.readLong()
    )
}