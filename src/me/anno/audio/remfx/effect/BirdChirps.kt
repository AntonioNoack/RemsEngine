package me.anno.audio.remfx.effect

import me.anno.audio.remfx.RemFXContext
import me.anno.maths.FastSin
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.mix
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class BirdChirps(
    var startHz: Float,
    var endHz: Float,
    var durationSeconds: Float,
    var sweepPower: Float = 1f,

    val delay: Float = 30f,
    spacing: Float = 1f, // 0 = no spacing, 1 = some spacing
    runs: Int = 3, // how many consecutive calls

    val harmonics: FloatArray = floatArrayOf(1f, 0.25f, 0.08f),
) : RandomizedSound() {

    val sin1 = FastSin()
    val sin2 = FastSin()
    val sin3 = FastSin()

    val spacingN = spacing + 1f
    val limitN = runs * spacingN

    var outerT = 0f
    var samplesToSkip = 0

    override fun add(
        context: RemFXContext,
        left: ShortArray,
        right: ShortArray,
    ) {
        val totalSamples = durationSeconds * context.sampleRate
        val invTotalSamples = 1f / totalSamples
        val (m1, m2, m3) = harmonics

        var innerT = outerT % spacingN

        var dx = 0f
        var envelope = 0f
        var ctr = -1

        var i = context.bufferOffset
        val end = i + context.bufferSize
        while (i < end) {
            if (samplesToSkip > 0) {
                val skip = min(end - i, samplesToSkip)
                i += skip
                samplesToSkip -= skip
                outerT = 0f
                innerT = 0f
                continue
            }

            if (outerT < limitN && innerT < 1f) {
                if (ctr.and(15) == 0) {
                    // eval every 16 frames is good enough
                    val sweepT = innerT.pow(sweepPower)
                    val hz = mix(startHz, endHz, sweepT + outerT * 0.03f)
                    dx = TAUf * hz / context.sampleRate

                    if (ctr.and(63) == 0) {
                        // Fast attack, gentle fade at the end.
                        // can we skip eval for this, too? 12 / 0.08 duration = 144hz, @44kHz -> 300 samples -> fine
                        val attack = min(innerT * 12f, 1f)
                        val release = min((1f - innerT) * 4f, 1f)
                        envelope = attack * release
                    }
                }

                val tone = sin1.next(dx) * m1 + sin2.next(dx * 2f) * m2 + sin3.next(dx * 3f) * m3
                add(context, tone * envelope, i, left, right)
                ctr++
            } else if (ctr != 0) {
                sin1.reset()
                sin2.reset()
                sin3.reset()
                ctr = 0

                if (outerT >= limitN) {
                    nextRun(totalSamples)
                    continue
                }
            }

            i++
            outerT += invTotalSamples
            innerT += spacingN
            if (outerT >= delay) {
                nextRun(totalSamples)
            } else if (innerT >= spacingN) {
                innerT -= spacingN
            }
        }
    }

    fun nextRun(totalSamples: Float) {
        // we can skip long runs here,
        //  we just need to know what to calculate
        val rnd = nextRandomFloat() * 0.5f + 0.5f
        samplesToSkip = max(((delay - outerT) * rnd * totalSamples).toInt(), 1)
    }
}
