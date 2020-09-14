package me.anno.ui.editor.files.thumbs

/**
 * 7
 * */
open class DatabaseCacheEntry7(
    val magicIdentifier: Magic,
    val cacheEntrySize: Int,
    val entryHash: Long,
    val filenameLength: Int,
    val paddingSize: Int,
    val dataSize: Int,
    val unknown: Int,
    val dataChecksum: Long,
    val headerChecksum: Long
) {
    constructor(input: WindowsThumbDBStream) : this(
        input.readMagic(), input.readInt(), input.readLong(), input.readInt(),
        input.readInt(), input.readInt(), input.readInt(), input.readLong(), input.readLong()
    )
}
