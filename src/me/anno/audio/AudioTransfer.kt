package me.anno.audio

import me.anno.utils.f2
import me.anno.utils.mix

open class AudioTransfer(val l2l: Double, val r2r: Double, val l2r: Double, val rl2: Double) {
    open fun l2l(f: Double, s: AudioTransfer) = mix(l2l, s.l2l, f)
    open fun r2r(f: Double, s: AudioTransfer) = mix(r2r, s.r2r, f)
    open fun l2r(f: Double, s: AudioTransfer) = mix(l2r, s.l2r, f)
    open fun r2l(f: Double, s: AudioTransfer) = mix(rl2, s.rl2, f)
    open fun getLeft(left: Double, right: Double, f: Double, s: AudioTransfer) =
        left * l2l(f, s) + right * r2l(f, s)

    open fun getRight(left: Double, right: Double, f: Double, s: AudioTransfer) =
        left * l2r(f, s) + right * r2r(f, s)

    override fun toString() = "[${l2l.f2()} ${r2r.f2()} ${l2r.f2()} ${rl2.f2()}]"
}