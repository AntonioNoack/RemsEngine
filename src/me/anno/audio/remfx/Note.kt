package me.anno.audio.remfx

import me.anno.maths.Maths.TAUf
import kotlin.math.pow

@Suppress("unused")
enum class Note {
    C,
    CSHARP,
    D,
    DSHARP,
    E,
    F,
    FSHARP,
    G,
    GSHARP,
    A,
    ASHARP,
    B;

    val frequency = 440f * 2f.pow((ordinal - 9) / 12f)
    val w = TAUf * frequency
    val symbol get() = symbols[ordinal]

    companion object {
        val symbols = "c,c#,d,d#,e,f,f#,g,a#,b".split(',')
    }
}