package ffmpeg.libavutil

object Common {

    fun MKTAG(a: Char, b: Char, c: Char, d: Char) = MKTAG(a.toInt(), b.toInt(), c.toInt(), d.toInt())
    fun MKTAG(a: Int, b: Int, c: Int, d: Int)   = a or b.shl(8) or c.shl(16) or d.shl(24)
    fun MKBETAG(a: Int, b: Int, c: Int, d: Int) = d or c.shl(8) or b.shl(16) or a.shl(24)

    
}