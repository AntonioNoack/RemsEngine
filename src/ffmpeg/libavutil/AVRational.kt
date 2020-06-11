package ffmpeg.libavutil

data class AVRational(val num: Int, val den: Int): Comparable<AVRational>{

    fun toDouble() = num/den.toDouble()
    fun invert() = AVRational(den, num)

    override fun compareTo(other: AVRational): Int = av_cmp_q(this, other)

    companion object {
        fun av_q2d(a: AVRational) = a.toDouble()
        fun av_inv_q(q: AVRational) = q.invert()
        fun av_cmp_q(a: AVRational, b: AVRational): Int{
            val tmp = a.num * b.den.toLong() - b.num * a.den.toLong()
            return if(tmp != 0L) (tmp xor a.den.toLong() xor b.den.toLong()).shr(63).toInt() or 1
            else if(b.den != 0 && a.den != 0) 0
            else if(a.num != 0 && b.num != 0) a.num.shr(31) - b.num.shr(31)
            else Int.MIN_VALUE
        }
    }

}