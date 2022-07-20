package me.anno.cache.keys

import me.anno.io.files.FileReference

data class VideoFramesKey(
    val file: FileReference,
    val scale: Int,
    val bufferIndex: Int,
    val frameLength: Int,
    val fps: Double
) {

    override fun toString(): String {
        return "$file, ${scale}x, $bufferIndex*$frameLength @${fps}fps"
    }

    val hashCode = generateHashCode()

    override fun hashCode(): Int = hashCode

    private fun generateHashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + scale
        result = 31 * result + bufferIndex
        result = 31 * result + frameLength
        result = 31 * result + fps.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VideoFramesKey) return false
        if (hashCode != other.hashCode) return false
        if (scale != other.scale) return false
        if (bufferIndex != other.bufferIndex) return false
        if (frameLength != other.frameLength) return false
        if (fps != other.fps) return false
        if (file != other.file) return false
        return true
    }

}