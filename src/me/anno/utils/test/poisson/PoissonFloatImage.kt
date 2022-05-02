package me.anno.utils.test.poisson

import me.anno.image.raw.FloatImage
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.utils.hpc.HeavyProcessing
import me.anno.video.VideoCreator
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * CPU implementation of poisson reconstruction
 * */
@Suppress("unused")
class PoissonFloatImage : Poisson<FloatImage> {

    private val tileSize = 32

    override fun FloatImage.next(): FloatImage {
        return FloatImage(width, height, numChannels)
    }

    override fun FloatImage.dx(dst: FloatImage): FloatImage {
        for (c in 0 until numChannels) {
            for (y in 0 until height) {
                var v0 = getValue(0, y, c)
                var v1 = v0
                for (x in 0 until width) {
                    val v2 = getValue(x + 1, y, c)
                    dst.setValue(x, y, c, v2 - v0)
                    v0 = v1
                    v1 = v2
                }
            }
        }
        return dst
    }

    override fun FloatImage.dy(dst: FloatImage): FloatImage {
        for (c in 0 until numChannels) {
            for (x in 0 until width) {
                var v0 = getValue(x, 0, c)
                var v1 = v0
                for (y in 0 until height) {
                    val v2 = getValue(x, y, c)
                    dst.setValue(x, y, c, v2 - v0)
                    v0 = v1
                    v1 = v2
                }
            }
        }
        return dst
    }

    override fun FloatImage.absDifference(other: FloatImage, dst: FloatImage): FloatImage {
        val d0 = data
        val d1 = other.data
        val ds = dst.data
        for (i in ds.indices) {
            ds[i] = kotlin.math.abs(d1[i] - d0[i])
        }
        return dst
    }

    override fun FloatImage.blurX(sigma: Float, dst: FloatImage): FloatImage {
        return convolveX(gaussianBlur(sigma), dst)
    }

    override fun FloatImage.blurY(sigma: Float, dst: FloatImage): FloatImage {
        return convolveY(gaussianBlur(sigma), dst)
    }

    override fun FloatImage.blurXSigned(sigma: Float, dst: FloatImage): FloatImage {
        return convolveX(sign(gaussianBlur(sigma)), dst)
    }

    override fun FloatImage.blurYSigned(sigma: Float, dst: FloatImage): FloatImage {
        return convolveY(sign(gaussianBlur(sigma)), dst)
    }

    private fun sign(mask: FloatArray): FloatArray {
        val center = mask.size shr 1
        mask[center] = 0f
        for (i in center + 1 until mask.size) {
            mask[i] = -mask[i]
        }
        return mask
    }

    private fun gaussianBlur(sigma: Float): FloatArray {
        val n = (sigma * 3f).roundToInt()
        val weights = FloatArray(n * 2 + 1) {
            val i = (it - n) / sigma
            exp(-i * i)
        }
        val factor = 1f / weights.sum()
        for (i in weights.indices) {
            weights[i] *= factor
        }
        return weights
    }

    private fun FloatImage.convolveX(weights: FloatArray, dst: FloatImage): FloatImage {
        val o = weights.size / 2
        HeavyProcessing.processBalanced2d(0, 0, width, height, tileSize, 4) { x0, y0, x1, y1 ->
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    for (c in 0 until numChannels) {
                        var sum = 0f
                        for (i in weights.indices) {
                            sum += weights[i] * getValue(x + i - o, y, c)
                        }
                        dst.setValue(x, y, c, sum)
                    }
                }
            }
        }
        return dst
    }

    private fun FloatImage.convolveY(weights: FloatArray, dst: FloatImage): FloatImage {
        val o = weights.size / 2
        HeavyProcessing.processBalanced2d(0, 0, width, height, tileSize, 4) { x0, y0, x1, y1 ->
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    for (c in 0 until numChannels) {
                        var sum = 0f
                        for (i in weights.indices) {
                            sum += weights[i] * getValue(x, y + i - o, c)
                        }
                        dst.setValue(x, y, c, sum)
                    }
                }
            }
        }
        return dst
    }

    override fun FloatImage.added(b: FloatImage, c: FloatImage, dst: FloatImage): FloatImage {
        HeavyProcessing.processBalanced2d(0, 0, width, height, tileSize, 4) { x0, y0, x1, y1 ->
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    for (ch in 0 until numChannels) {
                        dst.setValue(
                            x, y, ch,
                            getValue(x, y, ch) +
                                    b.getValue(x, y, ch) +
                                    c.getValue(x, y, ch)
                        )
                    }
                }
            }
        }
        return dst
    }

    override fun FloatImage.added(m: Float, n: Float, dst: FloatImage): FloatImage {
        HeavyProcessing.processBalanced2d(0, 0, width, height, tileSize, 4) { x0, y0, x1, y1 ->
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    for (c in 0 until numChannels) {
                        dst.setValue(x, y, c, m * getValue(x, y, c) + n)
                    }
                }
            }
        }
        return dst
    }

    override fun FloatImage.copyInto(dst: FloatImage): FloatImage {
        System.arraycopy(data, 0, dst.data, 0, dst.data.size)
        return dst
    }

    override fun iterate(
        src: FloatImage,
        dst: FloatImage,
        dx: FloatImage,
        dy: FloatImage,
        blurred: FloatImage
    ): FloatImage {
        val height = src.height
        val width = src.height
        val numChannels = src.numChannels
        HeavyProcessing.processBalanced2d(0, 0, width, height, tileSize, 4) { x0, y0, x1, y1 ->
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    for (c in 0 until numChannels) {
                        // compute better value for dst from src
                        val a0 = src.getValue(x, y, c)
                        val a1 = src.getValue(x - 2, y, c)
                        val a2 = src.getValue(x, y - 2, c)
                        val a3 = src.getValue(x + 2, y, c)
                        val a4 = src.getValue(x, y + 2, c)
                        val dxm = dx.getValue(x - 1, y, c)
                        val dxp = dx.getValue(x + 1, y, c)
                        val dym = dy.getValue(x, y - 1, c)
                        val dyp = dy.getValue(x, y + 1, c)
                        val t0 = ((a1 + a2 + a3 + a4) + (dxm - dxp) + (dym - dyp)) * 0.25f
                        val t1 = blurred.getValue(x, y, c) // stabilize iteration
                        dst.setValue(x, y, c, Maths.mix(a0, Maths.mix(t0, t1, 0.05f), 0.75f))
                    }
                }
            }
        }
        return dst
    }

    override fun FloatImage.renderVideo(iterations: Int, dst: FileReference, run: (Long) -> FloatImage) {
        VideoCreator.renderVideo2(
            width, height, 5.0,
            dst, 50, run
        )
    }

    override fun FloatImage.writeInto(dst: FileReference) {
        write(dst)
    }

}