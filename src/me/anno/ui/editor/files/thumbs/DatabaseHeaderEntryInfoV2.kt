package me.anno.ui.editor.files.thumbs

/**
 * 8v2
 * */
data class DatabaseHeaderEntryInfoV2(
    val unknown: Int,
    val firstCacheEntry: Int,
    val availableCacheEntry: Int,
    val numberOfCacheEntries: Int
) {
    constructor(input: WindowsThumbDBStream) : this(input.readInt(), input.readInt(), input.readInt(), input.readInt())
}