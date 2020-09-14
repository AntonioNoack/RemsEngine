package me.anno.ui.editor.files.thumbs

/**
 * 8v3, 8:1, 10
 * */
data class DatabaseHeaderEntryInfoV3(val unknown: Int, val firstCacheEntry: Int, val availableCacheEntry: Int) {
    constructor(input: WindowsThumbDBStream) : this(input.readInt(), input.readInt(), input.readInt())
}