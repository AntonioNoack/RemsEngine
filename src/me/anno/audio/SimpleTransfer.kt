package me.anno.audio

import me.anno.utils.types.Floats.f2

open class SimpleTransfer(l2l: Float, r2r: Float) : AudioTransfer(l2l, r2r, 0f, 0f) {

    override fun l2l(f: Float, s: AudioTransfer) = l2l * (1f - f) + f * s.l2l
    override fun r2r(f: Float, s:AudioTransfer) = r2r * (1f - f) + f * s.r2r
    override fun l2r(f: Float, s: AudioTransfer) = 0f
    override fun r2l(f: Float, s: AudioTransfer) = 0f

    override fun getLeft(left: Float, right: Float, f: Float, s: AudioTransfer) = left * (l2l * (1f - f) + f * s.l2l)
    override fun getRight(left: Float, right: Float, f: Float, s: AudioTransfer) = left * (r2r * (1f - f) + f * s.r2r)

    override fun toString() = "[${l2l.f2()} ${r2r.f2()} ${l2r.f2()} ${r2l.f2()}]"

    override fun multiply(s: Float) =
        when {
            s == 1f -> this
            s < 0.0003f -> ZeroTransfer
            else -> SimpleTransfer(l2l * s, r2r * s)
        }

    fun set(l: Float, r: Float): SimpleTransfer {
        l2l = l
        r2r = r
        return this
    }

    override fun set(other: AudioTransfer): SimpleTransfer {
        if(other === this) return this
        l2l = other.l2l
        r2r = other.r2r
        return this
    }

    override fun isZero(): Boolean = l2l == 0f && r2r == 0f

}