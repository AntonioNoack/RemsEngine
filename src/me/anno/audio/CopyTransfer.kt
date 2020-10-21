package me.anno.audio

object CopyTransfer: AudioTransfer(1.0, 1.0, 0.0, 0.0) {
    override fun l2l(f: Double, s: AudioTransfer) = 1.0
    override fun r2r(f: Double, s: AudioTransfer) = 1.0
    override fun l2r(f: Double, s: AudioTransfer) = 0.0
    override fun r2l(f: Double, s: AudioTransfer) = 0.0
    override fun getLeft(left: Double, right: Double, f: Double, s: AudioTransfer) = left
    override fun getRight(left: Double, right: Double, f: Double, s: AudioTransfer) = right
}