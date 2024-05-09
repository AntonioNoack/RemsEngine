package me.anno.mesh.vox

object VoxPalette {

    // we could store it in a large list, but this algorithm probably is shorter
    val defaultPalette = IntArray(256)

    init {

        val defaultPalette = defaultPalette
        var i = 1

        // {ff,cc,99,66,33,00}³
        for (b in 0 until 6) {
            val bv = (15 - b * 3)
            for (g in 0 until 6) {
                val gv = bv + (15 - g * 3).shl(8)
                for (r in 0 until 6) {
                    val rv = gv + (15 - r * 3).shl(16)
                    defaultPalette[i++] = rv * 0x11
                }
            }
        }

        // black comes last
        i--

        // extra tones with just one channel none-black
        for (channel in 0 until 4) {
            val mul = if (channel != 3) 0x11 shl (channel * 8) else 0x111111
            for (value in 14 downTo 1) {
                if (value % 3 != 0) {
                    defaultPalette[i++] = value * mul
                }
            }
        }
    }
}