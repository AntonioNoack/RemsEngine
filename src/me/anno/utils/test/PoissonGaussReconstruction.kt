package me.anno.utils.test

import me.anno.gpu.shader.ShaderLib
import me.anno.image.Image
import me.anno.image.ImageCPUCache
import me.anno.image.raw.FloatImage
import me.anno.maths.Maths.mix
import me.anno.utils.Clock
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures
import me.anno.utils.hpc.HeavyProcessing
import me.anno.video.VideoCreator.Companion.renderVideo2
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * testing poisson image reconstruction,
 * which is used in Gimp-Heal, Photoshop-Heal,
 * and a few ray tracing papers
 * */

// todo compute using gfx or compute shaders

const val tileSize = 32

fun brightness(rgb: Int) = ShaderLib.brightness(rgb.r01(), rgb.g01(), rgb.b01())

fun Image.grayscale(): FloatImage {
    val dst = FloatImage(width, height, 1)
    for (y in 0 until height) {
        for (x in 0 until width) {
            dst.setValue(x, y, 0, brightness(getRGB(x, y)))
        }
    }
    return dst
}

fun Image.floats(): FloatImage {
    val dst = FloatImage(width, height, 3)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = getRGB(x, y)
            dst.setValue(x, y, 0, color.r01())
            dst.setValue(x, y, 1, color.g01())
            dst.setValue(x, y, 2, color.b01())
        }
    }
    return dst
}

fun FloatImage.dx(): FloatImage {
    val dst = FloatImage(width, height, numChannels)
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

fun FloatImage.dy(): FloatImage {
    val dst = FloatImage(width, height, numChannels)
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

fun FloatImage.blur(sigma: Float): FloatImage {
    return blurX(sigma).blurY(sigma)
}

fun FloatImage.blurX(sigma: Float): FloatImage {
    return convolveX(gaussianBlur(sigma))
}

fun FloatImage.blurY(sigma: Float): FloatImage {
    return convolveY(gaussianBlur(sigma))
}

fun sign(mask: FloatArray): FloatArray {
    mask[mask.size / 2] = 0f
    for (i in 0 until mask.size / 2) {
        mask[i] = -mask[i]
    }
    return mask
}

fun FloatImage.blurXSigned(sigma: Float): FloatImage {
    return convolveX(sign(gaussianBlur(sigma)))
}

fun FloatImage.blurYSigned(sigma: Float): FloatImage {
    return convolveY(sign(gaussianBlur(sigma)))
}

fun gaussianBlur(sigma: Float): FloatArray {
    val n = (sigma * 3).roundToInt()
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

fun FloatImage.convolveX(weights: FloatArray): FloatImage {
    val dst = FloatImage(width, height, numChannels)
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

fun FloatImage.convolveY(weights: FloatArray): FloatImage {
    val dst = FloatImage(width, height, numChannels)
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

fun FloatImage.added(w: Float, other: FloatImage, wo: Float): FloatImage {
    val dst = FloatImage(width, height, numChannels)
    HeavyProcessing.processBalanced2d(0, 0, width, height, tileSize, 4) { x0, y0, x1, y1 ->
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                for (c in 0 until numChannels) {
                    dst.setValue(x, y, c, getValue(x, y, c) * w + other.getValue(x, y, c) * wo)
                }
            }
        }
    }
    return dst
}

fun FloatImage.added(m: Float, n: Float): FloatImage {
    val dst = FloatImage(width, height, numChannels)
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

// compute proper poisson reconstruction
// result loses contrast a little???...
fun iterate(src: FloatImage, dst: FloatImage, dx: FloatImage, dy: FloatImage, blurred: FloatImage): FloatImage {
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
                    dst.setValue(x, y, c, mix(a0, mix(t0, t1, 0.05f), 0.75f))
                }
            }
        }
    }
    return dst
}

fun main() {

    /*val image = FloatImage(3840, 2160, 1)
    HeavyProcessing.processBalanced2d(0, 0, image.width, image.height, tileSize, 1) { x0, y0, x1, y1 ->
        val id = Thread.currentThread().id.and(15) / 15f
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                image.setValue(x, y, 0, id)
            }
        }
    }
    image.write(desktop.getChild("test.jpg"))

    return*/

    // minimize(|current-gradient - target-gradient|² + alpha * |current-image - blurred-image|²

    // an idea: can we reconstruct the image just with gaussian blurs?
    // answer: not in this simple way, we get star artefacts
    // load image
    // compute dx,dy
    // gaussian-blur image
    // reconstruct original image: blurred + Integral(dx,dy), where Integral is just accumulated differences

    val src = pictures.getChild("bg,f8f8f8-flat,750x,075,f-pad,750x1000,f8f8f8.u4.jpg")
    val ext = ".png"
    val original = ImageCPUCache.getImage(src, false)!!.floats()
    val dst = desktop.getChild("poisson")
    dst.tryMkdirs()

    val clock = Clock()

    val dx = original.dx()
    val dy = original.dy()

    clock.stop("delta")

    val sigma = 6f

    val blurred = original.blur(sigma)

    // would need to be blurred, and not blurred at the same time...
    // how?
    val bdx = dx.blurXSigned(sigma) // .blurY(sigma)
    val bdy = dy.blurYSigned(sigma) // .blurX(sigma)

    val errorScale = 1f
    val normalScale = 1f

    clock.stop("convolve")

    val result = blurred
        .added(1f, bdx, -1f)
        .added(1f, bdy, -1f)

    val error = original
        .added(errorScale, result, -errorScale)
        .abs()

    clock.stop("error & result")

    blurred.write(dst.getChild("blurred.jpg"))

    dx.added(normalScale, 0.5f).write(dst.getChild("dx$ext"))
    dy.added(normalScale, 0.5f).write(dst.getChild("dy$ext"))

    bdx.added(normalScale, 0.5f).write(dst.getChild("bdx$ext"))
    bdy.added(normalScale, 0.5f).write(dst.getChild("bdy$ext"))

    result.write(dst.getChild("trick$ext"))

    error.write(dst.getChild("error-gaussian$ext"))

    val tmp = result.clone()
    renderVideo2(
        original.width, original.height, 5.0,
        dst.getChild("trick-to-result.mp4"), 50
    ) {
        if (it > 0L) {
            val s = if (it.and(1) == 1L) result else tmp
            val d = if (s == result) tmp else result
            iterate(s, d, dx, dy, blurred)
        } else result
    }

    result.write(dst.getChild("result-trick$ext"))

    val error0 = original
        .added(errorScale, result, -errorScale)
        .abs()

    error0.write(dst.getChild("error-trick$ext"))

    System.arraycopy(blurred.data, 0, result.data, 0, result.data.size)

    renderVideo2(
        original.width, original.height, 5.0,
        dst.getChild("blurred-to-result.mp4"), 50
    ) {
        if (it > 0L) {
            val s = if (it.and(1) == 1L) result else tmp
            val d = if (s == result) tmp else result
            iterate(s, d, dx, dy, blurred)
        } else result
    }

    result.write(dst.getChild("result-blurred$ext"))

    val error1 = original
        .added(errorScale, result, -errorScale)
        .abs()

    error1.write(dst.getChild("error-blurred$ext"))

    // a test of when src == dst, so the improvement gets passed along
    /*System.arraycopy(blurred.data, 0, result.data, 0, result.data.size)

    renderVideo2(
        original.width, original.height, 5.0,
        dst.getChild("fromBlurred2.mp4"), 50
    ) { if (it > 0L) iterate(result, result, dx, dy, blurred) else result }

    result.write(dst.getChild("result-blurred2.jpg"))*/

    clock.stop("writing results")

}