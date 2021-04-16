package me.anno.cache.keys

import java.io.File

data class AudioSliceKey(val file: File, val slice: Long){
    val hashCode = (file.hashCode()*31) or slice.hashCode()
    override fun hashCode() = hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is AudioSliceKey) return false

        if (slice != other.slice) return false
        if (file != other.file) return false

        return true
    }
}