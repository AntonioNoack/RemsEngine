package me.anno.video

import me.anno.io.files.FileReference
import me.anno.utils.strings.StringHelper.shorten

data class VideoFramesKey(
    val file: FileReference,
    val scale: Int,
    val bufferIndex: Int,
    val frameLength: Int,
    val fps: Double
) {

    override fun toString(): String {
        return "${file.absolutePath.shorten(200)}, ${scale}x, $bufferIndex*$frameLength @${fps}fps"
    }

    private val _hashCode = generateHashCode()
    override fun hashCode(): Int = _hashCode

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
        if (_hashCode != other._hashCode) return false
        if (scale != other.scale) return false
        if (bufferIndex != other.bufferIndex) return false
        if (frameLength != other.frameLength) return false
        if (fps != other.fps) return false
        if (file != other.file) return false
        return true
    }

}