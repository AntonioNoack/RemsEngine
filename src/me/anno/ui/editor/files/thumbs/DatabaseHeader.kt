package me.anno.ui.editor.files.thumbs

data class DatabaseHeader(val magic: Magic, val version: Int, val type: Int) {
    constructor(input: WindowsThumbDBStream) : this(input.readMagic(), input.readInt(), input.readInt())
}