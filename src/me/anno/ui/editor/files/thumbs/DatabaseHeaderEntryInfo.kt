package me.anno.ui.editor.files.thumbs

/**
 * vista/7/8
 * */
data class DatabaseHeaderEntryInfo(
    val firstCacheEntry: Int,
    val availableCacheEntry: Int,
    val numberOfCacheEntries: Int
) {
    constructor(input: WindowsThumbDBStream) : this(input.readInt(), input.readInt(), input.readInt())
}