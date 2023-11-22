package me.anno.audio

import me.anno.io.files.FileReference

data class AudioSliceKey(val file: FileReference, val slice: Long) {
    private val _hashCode = (file.hashCode() * 31) or slice.hashCode()
    override fun hashCode() = _hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is AudioSliceKey) return false

        if (slice != other.slice) return false
        if (file != other.file) return false

        return true
    }
}