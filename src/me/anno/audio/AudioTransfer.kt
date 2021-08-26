package me.anno.audio

import me.anno.utils.maths.Maths.mix
import me.anno.utils.types.Floats.f2

open class AudioTransfer(var l2l: Float, var r2r: Float, var l2r: Float, var r2l: Float) {

    open fun l2l(f: Float, s: AudioTransfer) = mix(l2l, s.l2l, f)
    open fun r2r(f: Float, s: AudioTransfer) = mix(r2r, s.r2r, f)
    open fun l2r(f: Float, s: AudioTransfer) = mix(l2r, s.l2r, f)
    open fun r2l(f: Float, s: AudioTransfer) = mix(r2l, s.r2l, f)

    open fun getLeft(left: Float, right: Float, f: Float, s: AudioTransfer) =
        left * l2l(f, s) + right * r2l(f, s)

    open fun getRight(left: Float, right: Float, f: Float, s: AudioTransfer) =
        left * l2r(f, s) + right * r2r(f, s)

    override fun toString() = "[${l2l.f2()} ${r2r.f2()} ${l2r.f2()} ${r2l.f2()}]"

    open fun multiply(s: Float) =
        when{
            s == 1f -> this
            s < 0.0003f -> ZeroTransfer
            else -> AudioTransfer(l2l * s, r2r * s, l2r * s, r2l * s)
        }

    open fun set(other: AudioTransfer): AudioTransfer {
        l2l = other.l2l
        r2r = other.r2r
        l2r = other.l2r
        r2l = other.r2l
        return this
    }

    open fun isZero() = l2l == 0f && l2r == 0f && r2r == 0f && r2l == 0f

}