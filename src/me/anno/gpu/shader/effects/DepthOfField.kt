package me.anno.gpu.shader.effects

import me.anno.ecs.components.mesh.sdf.shapes.SDFHeart
import me.anno.image.raw.FloatImage
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Optimization
import me.anno.utils.OS.desktop
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBd
import org.joml.Vector2d
import org.joml.Vector4f
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

// todo near blurred with coc is single image at largest coc:
// todo blurring there is wanted

// todo in focus has no blurring

// todo far buffers, maybe multiples, rendered without the front objects to avoid bleeding

object DepthOfField {

    fun circleOfConfusion(
        aperture: Float, focalLength: Float, objectDistance: Float,
        planeInFocus: Float
    ) =
        abs(aperture * (focalLength * (objectDistance - planeInFocus)) / (objectDistance * (planeInFocus - focalLength)))

    fun objectDistance(z: Float, zNear: Float, zFar: Float) = -zFar * zNear / (z * (zFar - zNear) - zFar)

    fun circleOfConfusion2(z: Float, cocScale: Float, cocBias: Float) = abs(z * cocScale + cocBias)

    fun cocScale(aperture: Float, focalLength: Float, planeInFocus: Float, zNear: Float, zFar: Float) =
        (aperture * focalLength * planeInFocus * (zFar - zNear)) / ((planeInFocus - focalLength) * zNear * zFar)

    fun cocBias(aperture: Float, focalLength: Float, planeInFocus: Float, zNear: Float, zFar: Float) =
        (aperture * focalLength * (zNear - planeInFocus)) / ((planeInFocus * focalLength) * zNear)

    // todo create a dof effect:
    // todo create some, 3? textures:
    // todo pure, 1/4, 1/16

    // todo we additionally need access to the depth texture

    @JvmStatic
    fun main(args: Array<String>) {

        // todo gradient descent to create kernels for any input target 😍
        //  -> we could create Bokeh with hearts ♥
        //  High-Performance Image Filters via Sparse Approximations

        val numLayers = 3
        val totalSamples = 50

        val r = 10
        val dst = FloatImage(r * 2 + 1, r * 2 + 1, 1)
        val h = SDFHeart()
        val w = Vector4f()
        val s = IntArrayList(1)
        h.scale = r * 1.7f
        for (dx in -r..r) {
            for (dy in -r..r) {
                val v = -h.computeSDF(w.set(dx.toFloat(), -dy.toFloat(), 0f, 1f), s)
                dst.setValue(r + dx, r + dy, 0, clamp(v))
            }
        }
        dst.normalize().write(desktop.getChild("target.png"))

        val rx = max(dst.width, dst.height).toDouble() * 5.0 / numLayers
        val rnd = Random()
        val kernels = Array(numLayers) { li ->
            val i0 = li * totalSamples / numLayers
            val i1 = (li + 1) * totalSamples / numLayers
            val c = i1 - i0
            Array(c) {
                Sampler(
                    Vector2d(
                        rx * rnd.nextGaussian(),
                        rx * rnd.nextGaussian()
                    ), 10.0
                )
            }
        }

        val allSamplers = kernels.flatten()
        val data = DoubleArray(totalSamples * 3)

        for (j in 0 until totalSamples) {
            val sampler = allSamplers[j]
            val k = j * 3
            data[k + 0] = sampler.position.x
            data[k + 1] = sampler.position.y
            data[k + 2] = sampler.weight
        }

        fun fill(it: DoubleArray) {
            for (j in 0 until totalSamples) {
                val sampler = allSamplers[j]
                val k = j * 3
                val wi = max(it[k + 2], 0.0)
                sampler.position.set(it[k], it[k + 1])
                sampler.weight = wi
            }
        }

        fill(data)

        var first = true
        var err = 0.0
        var res: DoubleArray = data
        for (i in 0 until 10) {
            println("// $i")
            val (_, res2) = Optimization.randomSearch(res, 1.0, 0.0, 100000, 1000) {
                fill(it)
                val src = sparseConvolve(kernels, false)
                // eval error
                val error = error(src, dst)
                if (first || Math.random() < 1e-3) {
                    println(error)
                    first = false
                }
                error
            }
            val (err3, res3) = Optimization.simplexAlgorithm(res2, 0.01, 0.0, 100000) {
                fill(it)
                val src = sparseConvolve(kernels, false)
                // eval error
                val error = error(src, dst)
                if (first || Math.random() < 1e-3) {
                    println(error)
                    first = false
                }
                error
            }
            err = err3
            res = res3
        }

        fill(res)

        println("$err, ${allSamplers.joinToString()}")

        sparseConvolve(kernels, true)

    }
}

class Sampler(val position: Vector2d, var weight: Double) {
    override fun toString() = "[(${position.x},${position.y})x$weight]"
}
typealias SparseKernel = Array<Sampler>

fun error(src: FloatImage, dst: FloatImage): Double {
    val w = max(src.width, dst.width)
    val h = max(src.height, dst.height)
    val s0 = pad(src, w, h)
    val d0 = pad(dst, w, h)
    var error = 0.0
    for (i in 0 until w * h) {
        val err1 = s0[i] - d0[i]
        error += err1 * err1
    }
    return error
}

fun pad(src: FloatImage, w: Int, h: Int): FloatArray {
    val dxs = (w - src.width) / 2
    val dys = (h - src.height) / 2
    if (dxs <= 0 && dys <= 0) return src.data
    val dst = FloatArray(w * h)
    val src1 = src.data
    var i = 0
    val i0 = dxs + dys * w
    for (y in 0 until src.height) {
        for (x in 0 until src.width) {
            dst[i0 + x + y * w] = src1[i++]
        }
    }
    return dst
}

fun sparseConvolve(kernels: Array<SparseKernel>, write: Boolean): FloatImage {
    // convolve
    val bounds = AABBd()
    val numLayers = kernels.size
    var image = FloatImage(1, 1, 1, floatArrayOf(1f))
    for (i in 0 until numLayers) {
        val k = kernels[i]
        // calculate extra bounds :)
        bounds.clear()
        for (j in k.indices) {
            val p = k[j].position
            val e = p.length() * 1e-3
            bounds.union(p.x - e, p.y - e, 0.0)
            bounds.union(p.x + e, p.y + e, 0.0)
        }
        // calculate extra size
        val ex = ceil(max(bounds.maxX, -bounds.minX)).toInt() * 2
        val ey = ceil(max(bounds.maxY, -bounds.minY)).toInt() * 2
        val tmp = FloatImage(image.width + ex, image.height + ey, 1)
        // convolve actually
        sparseConvolve(image, tmp, k)
        if (write) {
            sparseConvolve(arrayOf(k), false).normalize().write(desktop.getChild("img/krn$i.png"))
            tmp.normalize().write(desktop.getChild("img/img$i.png"))
        }
        image = tmp
    }
    return image.normalize()
}

fun sparseConvolve(src: FloatImage, dst: FloatImage, kernel: SparseKernel) {
    val ox = (dst.width - src.width) ushr 1
    val oy = (dst.height - src.height) ushr 1
    val sd = src.data
    val dw = dst.width
    val dd = dst.data
    var si = 0
    for (sy in 0 until src.height) {
        for (sx in 0 until src.width) {
            val sv = sd[si++]
            if (sv != 0f) {
                for (ki in kernel.indices) {
                    val k = kernel[ki]
                    val p = k.position
                    val dv = (sv * k.weight).toFloat()
                    val dx = (sx + ox) + p.x.toFloat()
                    val dy = (sy + oy) + p.y.toFloat()
                    val xi0 = floor(dx)
                    val yi0 = floor(dy)
                    val xi = xi0.toInt()
                    val yi = yi0.toInt()
                    val xf1 = dx - xi0
                    val yf1 = dy - yi0
                    val xf0 = 1f - xf1
                    val yf0 = 1f - yf1
                    val di = xi + yi * dw
                    dd[di] += dv * xf0 * yf0
                    dd[di + 1] += dv * xf1 * yf0
                    dd[di + dw] += dv * xf0 * yf1
                    dd[di + dw + 1] += dv * xf1 * yf1
                }
            }
        }
    }
}