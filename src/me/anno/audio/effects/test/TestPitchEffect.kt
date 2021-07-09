package me.anno.audio.effects.test

import me.anno.animation.AnimatedProperty
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.length
import me.anno.utils.OS
import org.jtransforms.fft.FloatFFT_1D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*

class TestPitchEffect(
    val blockSize: Int,
    val pitch: AnimatedProperty<Float>,
    val dtPerBuffer: Double,
    val time0: Double
) {

    val piX2 = (2.0 * Math.PI).toFloat()

    val half = blockSize / 2
    var bufferIndex = 0

    fun mirroredIndex(i: Int): Int {
        return if (i >= half) {
            blockSize - (i + 1)
        } else i
    }

    val phaseOffset = FloatArray(blockSize)
    val amplitudeMultiplier = 2f / blockSize

    /**
     * applies the pitch effect on the sound data
     * O(blockSize²)
     * */
    fun apply(data: FloatArray): FloatArray {

        val fft = FloatFFT_1D(blockSize.toLong())
        fft.realForward(data)

        val isFirstBuffer = bufferIndex == 0
        val timeX0 = time0 + dtPerBuffer * bufferIndex

        val result = FloatArray(blockSize)

        val timeIntegral = FloatArray(blockSize)
        timeIntegral[0] = pitch[timeX0]

        for(i in 1 until blockSize){
            timeIntegral[i] = timeIntegral[i-1] + pitch[timeX0 + i * dtPerBuffer / blockSize]
        }

        for (frequency in 1 until blockSize / 2) {
            val fi = mirroredIndex(frequency)
            val r = data[fi * 2]
            val i = data[fi * 2 + 1]
            val amplitude0 = length(r, i) * amplitudeMultiplier
            if (abs(amplitude0) > 1e-5f && frequency == 4) {
                val baseFrequency = (frequency * 2 * PI).toFloat() / blockSize
                val phase0 = if (isFirstBuffer) atan2(i, r) else phaseOffset[frequency]
                for (t in 0 until blockSize) {
                    result[t] += cos((phase0 + timeIntegral[t]) * baseFrequency) * amplitude0
                }
                phaseOffset[frequency] = (phase0 + timeIntegral.last()) % (piX2 / baseFrequency)
            } else phaseOffset[frequency] = 0f
        }

        bufferIndex++

        return result

    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {

            val blockSize = 256
            val pitch = AnimatedProperty.float(0.7f)
            val fft = TestPitchEffect(blockSize, pitch, 1.0, 0.0)

            fun showWaves(data: FloatArray, name: String) {
                val img = BufferedImage(blockSize, blockSize, 1)
                val amplitude = blockSize / (2f * data.map { abs(it) }.maxOrNull()!!)
                for (i in 0 until blockSize) {
                    val y = clamp(blockSize / 2 + (amplitude * data[i]).roundToInt(), 0, blockSize - 1)
                    for (j in 0 until y) {
                        img.setRGB(i, j, -1)
                    }
                }
                ImageIO.write(img, "png", OS.desktop.getChild(name).outputStream())
            }

            fun compare(input: FloatArray) {
                val input0 = FloatArray(input.size) { input[it] }
                val output = fft.apply(input)
                /*for (i in 0 until blockSize) {
                    LOGGER.info("${input0[i].f3()} -> ${output[i].f3()}")
                }*/
                showWaves(input0, "i.${fft.bufferIndex}.in.png")
                showWaves(output, "i.${fft.bufferIndex}.out.png")
            }

            compare(FloatArray(blockSize) { cos(it * 3.75f * Math.PI.toFloat() * 2f / blockSize) })
            compare(FloatArray(blockSize) { cos((it + blockSize) * 3.75f * Math.PI.toFloat() * 2f / blockSize) })
            compare(FloatArray(blockSize) { cos((it + 2 * blockSize) * 3.75f * Math.PI.toFloat() * 2f / blockSize) })

        }

    }

}