package me.anno.audio.remfx.effect

import me.anno.audio.remfx.RemFXContext
import me.anno.maths.FastSin
import me.anno.maths.Maths.TAUf
import kotlin.math.min

class WoodpeckerDrum(
    private val strikesPerSecond: Float = 16f,
    private val strikesPerBurst: Int = 12,
    private val burstGapSeconds: Float = 1.2f,
    private val gapJitter: Float = 0.35f, // 0 = steady repeat, 0.35 = ±35%
    private val hitSeconds: Float = 0.010f,
    private val bodyHz: Float = 700f,
) : RandomizedSound() {

    private val sin = FastSin()

    private var samplesToSkip = 0
    private var hitCtr = 0
    private var t = -1f
    private var amplitude = 1f

    override fun add(
        context: RemFXContext,
        left: ShortArray,
        right: ShortArray,
    ) {
        val sampleRate = context.sampleRate
        val dx = TAUf * bodyHz / sampleRate

        val hitLengthInvSamples = 1f / (hitSeconds * sampleRate)
        val baseSpacing = sampleRate / strikesPerSecond
        val burstGapSamples = burstGapSeconds * sampleRate

        var i = context.bufferOffset
        val end = i + context.bufferSize

        while (i < end) {
            // Skip silence until the next peck, but keep the absolute clock moving.
            if (t < 0f && samplesToSkip > 0) {
                val skipI = min(samplesToSkip, end - i)
                samplesToSkip -= skipI
                i += skipI
                continue
            }

            if (t < 0f) {
                t = 0f
                amplitude = 0.8f + nextRandomFloat() * 0.2f
                sin.reset()
            }

            // Short noisy impact, with a decaying resonant body underneath it.
            val envelope = 1f - t
            val noise = nextRandomFloat()
            val resonance = sin.next(dx)

            val sound = (noise * 0.75f + resonance * 0.25f) * envelope * amplitude
            add(context, sound, i, left, right)

            t += hitLengthInvSamples
            i++

            if (t >= 1f) {
                t = -1f
                sin.reset()

                hitCtr++
                if (hitCtr < strikesPerBurst) {
                    // Slightly vary peck spacing; keep it recognizably a roll.
                    val jitter = (nextRandomFloat() * 2f - 1f) * 0.08f
                    samplesToSkip = (baseSpacing * (1f + jitter)).toInt()
                } else {
                    hitCtr = 0
                    val jitter = (nextRandomFloat() * 2f - 1f) * gapJitter
                    samplesToSkip = (burstGapSamples * (1f + jitter)).toInt()
                }
            }
        }

        sin.normalize()
    }
}
