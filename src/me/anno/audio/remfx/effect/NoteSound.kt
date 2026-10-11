package me.anno.audio.remfx.effect

import me.anno.audio.AudioCache.playbackSampleRate
import me.anno.audio.remfx.Note
import me.anno.audio.remfx.RemFXContext
import me.anno.maths.FastSin
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.fract
import kotlin.math.pow
import kotlin.math.sin

class NoteSound(frequency: Float) : Sound() {

    constructor(note: Note, octave: Int = 0) :
            this(note.frequency * 2f.pow(octave))

    val w = TAUf * frequency / playbackSampleRate

    // slow sounds scary; 0.01 is definitely noticeable; gives it a swingy sound, e.g. like a guitar
    var vibratoStrength = 0f
    var vibratoFrequency = 15f

    // 0.03 is noticeable
    var tremoloStrength = 0f
    var tremoloFrequency = 5f

    val sin1 = FastSin()
    val sin2 = FastSin()
    val sin3 = FastSin()

    val harmonics = floatArrayOf(0.7f, 0.2f, 0.1f)

    override fun add(context: RemFXContext, left: ShortArray, right: ShortArray) {

        val i0 = context.sampleOffset
        val tremoloPhase = TAUf * fract(i0.toDouble() / context.sampleRate * tremoloFrequency).toFloat()
        val vibratoPhase = TAUf * fract(i0.toDouble() / context.sampleRate * vibratoFrequency).toFloat()
        val volumeMod = 1f + (tremoloStrength * sin(tremoloPhase))
        val speedMod = 1f + (vibratoStrength * sin(vibratoPhase))

        val wx = w * speedMod
        val wx2 = wx * 2f
        val wx3 = wx * 3f

        val (m1, m2, m3) = harmonics
        for (i in context.indices) {
            val sound = sin1.next(wx) * m1 + sin2.next(wx2) * m2 + sin3.next(wx3) * m3
            add(context, sound * volumeMod, i, left, right)
        }

        sin1.normalize()
        sin2.normalize()
        sin3.normalize()
    }
}