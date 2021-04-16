package me.anno.audio

object ZeroTransfer : AudioTransfer(0f, 0f, 0f, 0f) {
    override fun multiply(s: Float): AudioTransfer {
        return this
    }
    override fun isZero(): Boolean = true
}