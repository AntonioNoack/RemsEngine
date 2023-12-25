package me.anno.tests.maths

import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.raw.FloatImage
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures
import kotlin.math.abs
import kotlin.math.exp

class Kernel(
    val sx: Int, val sy: Int, val sz: Int = 1,
    val values: DoubleArray = DoubleArray(sx * sy * sz),
    val offset: Int = 0
) {

    constructor(sx: Int, sy: Int, v: DoubleArray) : this(sx, sy, 1, v)

    constructor(image: Image) : this(image.width, image.height, image.numChannels) {
        var i = 0
        if (sz > 0) for (y in 0 until sy) {
            for (x in 0 until sx) {
                values[i++] = image.getRGB(x, y).r01().toDouble()
            }
        }
        if (sz > 1) for (y in 0 until sy) {
            for (x in 0 until sx) {
                values[i++] = image.getRGB(x, y).g01().toDouble()
            }
        }
        if (sz > 2) for (y in 0 until sy) {
            for (x in 0 until sx) {
                values[i++] = image.getRGB(x, y).b01().toDouble()
            }
        }
        if (sz > 3) for (y in 0 until sy) {
            for (x in 0 until sx) {
                values[i++] = image.getRGB(x, y).a01().toDouble()
            }
        }
    }

    val sxy = sx * sy

    init {
        if (sx * sy * sz + offset > values.size)
            throw IllegalArgumentException()
    }

    operator fun get(x: Int, y: Int, z: Int): Double {
        return if (x in 0 until sx && y in 0 until sy && z in 0 until sz) {
            values[x + y * sx + z * sxy + offset]
        } else 0.0
    }

    operator fun set(x: Int, y: Int, z: Int, v: Double) {
        values[x + y * sx + z * sxy + offset] = v
    }

    fun apply(image: Kernel, dsx: Int = image.sx, dsy: Int = image.sy, dsz: Int = image.sz): Kernel {
        val dst = Kernel(dsx, dsy, dsz)
        val dx = image.sx - dst.sx
        val dy = image.sy - dst.sy
        val dz = image.sz - dst.sz
        for (z in 0 until dsz) {
            for (y in 0 until dsy) {
                for (x in 0 until dsx) {
                    var sum = 0.0
                    val x0 = x + dx
                    val y0 = y + dy
                    val z0 = z + dz
                    var i = 0
                    for (zi in 0 until sz) {
                        for (yi in 0 until sy) {
                            for (xi in 0 until sx) {
                                sum += image[xi + x0, yi + y0, zi + z0] * values[i++]
                            }
                        }
                    }
                    dst[x, y, z] = sum
                }
            }
        }
        return dst
    }

    fun mul(other: Kernel): Kernel {
        val nx = sx + other.sx - 1
        val ny = sy + other.sy - 1
        val nz = sz + other.sz - 1
        return apply(other, nx, ny, nz)
    }

    fun write(dst: FileReference, scale: Int = 8, normalize: Boolean = true): Kernel {
        val img = if (scale > 1) {
            val sx2 = sx * scale
            val sy2 = sy * scale
            // convert planes to channels
            val v = FloatArray(sx2 * sy2 * sz)
            var i = 0
            for (y in 0 until sy2) {
                val y2 = y / scale
                for (x in 0 until sx2) {
                    val x2 = x / scale
                    for (z in 0 until sz) {
                        v[i++] = this[x2, y2, z].toFloat()
                    }
                }
            }
            FloatImage(sx2, sy2, sz, v)
        } else {
            // convert planes to channels
            val v = FloatArray(sx * sy * sz)
            var i = 0
            for (y in 0 until sy) {
                for (x in 0 until sx) {
                    for (z in 0 until sz) {
                        v[i++] = this[x, y, z].toFloat()
                    }
                }
            }
            FloatImage(sx, sy, sz, v)
        }
        if (normalize) img.normalize()
        img.write(dst)
        return this
    }

    fun transposed(): Kernel {
        if (sz != 1) throw NotImplementedError()
        return if (sx == 1 || sy == 1) {
            Kernel(sy, sx, values)
        } else {
            Kernel(sy, sx, DoubleArray(sx * sy) {
                val x = it % sy
                val y = it / sy
                this[y, x, 0]
            })
        }
    }

    fun add(other: Kernel): Kernel {
        val v = DoubleArray(sx * sy)
        values.copyInto(v, 0, 0, v.size)
        for (i in v.indices) {
            v[i] += other.values[i]
        }
        return Kernel(sx, sy, v)
    }

    fun mix(other: Kernel, f: Double): Kernel {
        val v = DoubleArray(sx * sy)
        values.copyInto(v, 0, 0, v.size)
        for (i in v.indices) {
            v[i] = Maths.mix(v[i], other.values[i], f)
        }
        return Kernel(sx, sy, v)
    }

    operator fun get(z: Int): Kernel {
        return Kernel(sx, sy, 1, values, sxy * z)
    }

}

fun stack(vararg kernels: Kernel): Kernel {
    val sx = kernels.maxOf { it.sx }
    val sy = kernels.maxOf { it.sy }
    val sz = kernels.sumOf { it.sz }
    val v = DoubleArray(sx * sy * sz)
    var z = 0
    val sxy = sx * sy
    for (kernel in kernels) {
        val dx = (sx - kernel.sx) / 2
        val dy = (sy - kernel.sy) / 2
        for (zi in 0 until kernel.sz) {
            for (yi in 0 until kernel.sy) {
                var i = dx + (yi + dy) * sx + z * sxy
                for (xi in 0 until kernel.sx) {
                    v[i++] = kernel[xi, yi, zi]
                }
            }
            z++
        }
    }
    return Kernel(sx, sy, sz, v)
}

fun main() {

    val dst = desktop.getChild("kernel")
    dst.mkdirs()

    // test gaussian
    val gaussian1x3 = Kernel(1, 3, doubleArrayOf(0.25, 1.0, 0.25))
    val gaussian3x1 = Kernel(3, 1, doubleArrayOf(0.25, 1.0, 0.25))

    val gaussian3x3 = gaussian1x3.mul(gaussian3x1).write(dst.getChild("3x3.png"))
    val gaussian5x1 = gaussian3x1.mul(gaussian3x1).write(dst.getChild("5x1.png"))
    val gaussian1x5 = gaussian1x3.mul(gaussian1x3).write(dst.getChild("1x5.png"))
    val gaussian5x5v1 = gaussian3x3.mul(gaussian3x3).write(dst.getChild("5x5v1.png"))
    val gaussian5x5v2 = gaussian1x5.mul(gaussian5x1).write(dst.getChild("5x5v2.png"))

    val gaussian9x9 = gaussian5x5v1.mul(gaussian5x5v1).write(dst.getChild("9x9.png"))
    val gaussian17x17 = gaussian9x9.mul(gaussian9x9).write(dst.getChild("17x17.png"))

    val kdx = Kernel(3, 1, doubleArrayOf(-1.0, 0.0, 1.0))
    val kdy = Kernel(1, 3, doubleArrayOf(-1.0, 0.0, 1.0))

    // test dx*dy, dy*dx
    // val kdxdy = kdx.mul(kdy).write(dst.getChild("dxdy.png"))
    // val kdydx = kdy.mul(kdx).write(dst.getChild("dydx.png"))

    // box blur to gaussian
    val box = Kernel(7, 1, DoubleArray(7) { 1.0 })
    box.mul(box).mul(box).write(dst.getChild("boxToGaussian1d.png"))
    val box2d = box.mul(box.transposed())
    box2d.mul(box2d).mul(box2d).write(dst.getChild("boxToGaussian2d.png"))

    // todo test poisson reconstruction
    val defaultSigma = 3.0
    fun gaussian(i: Int, sigma: Double = defaultSigma): Double {
        return exp(-(i * i) * sigma)
    }

    fun gaussianX(s: Int): Kernel {
        val dx = s / 2
        val sigma = defaultSigma / (dx * dx)
        return Kernel(s, 1, DoubleArray(s) {
            gaussian(it - dx, sigma)
        })
    }

    fun gaussianY(s: Int) = gaussianX(s).transposed()

    val g17x = gaussianX(17)
    val g17y = g17x.transposed().write(dst.getChild("transpose.png"))

    g17x.mul(kdx).write(dst.getChild("g17dx.png"))
    // g17x.mul(kdx).mul(kdy).write(dst.getChild("g17dxdy.png"))

    val poissonColor = Kernel(
        5, 5, doubleArrayOf(
            0.0, 0.0, 0.25, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0.0,
            0.25, 0.0, 0.0, 0.0, 0.25,
            0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.25, 0.0, 0.0,
        )
    )

    val poissonDx = Kernel(3, 1, doubleArrayOf(0.25, 0.0, -0.25))
    val poissonDy = poissonDx.transposed()

    val sx = 25
    val sy = 25
    val iter = 15
    val dstImage = Kernel(sx, sy, 3)
    for (x in 0 until 25) {
        for (y in 0 until 25) {
            for (z in 0 until 3) {
                val image = Kernel(sx, sy, 3)
                image[x, y, z] = 1.0
                for (i in 0 until iter) {
                    // apply one iteration
                    val color = poissonColor.apply(image[0])
                    val dx = poissonDx.apply(image[1])
                    val dy = poissonDy.apply(image[2])
                    for (yi in 0 until sy) {
                        for (xi in 0 until sx) {
                            image[xi, yi, 0] = color[xi, yi, 0] + dx[xi, yi, 0] + dy[xi, yi, 0]
                        }
                    }
                }
                // read out center
                dstImage[x, y, z] = image[sx / 2, sy / 2, 0]
            }
        }
    }
    for (v in dstImage.values.indices) {
        dstImage.values[v] = abs(dstImage.values[v])
    }
    dstImage.write(dst.getChild("poissonTest.png"))

    val src = ImageCache[pictures.getChild("bg,f8f8f8-flat,750x,075,f-pad,750x1000,f8f8f8.u4.jpg"), false]!!
    Kernel(2, 1, doubleArrayOf(-1.0, 1.0)).mul(Kernel(src))
        .write(dst.getChild("dxTest.png"))

/*
    // this operation is not correct, I think...
    val poisson = stack(poissonColor, poissonDx, poissonDy)
    poisson.write(dst.getChild("poisson.png"))
    poisson
        .mul(poisson)
        .mul(poisson)
        .mul(poisson)
        .mul(poisson)
        .mul(poisson)
        .mul(poisson)
        .mul(poisson)
        .mul(poisson)
        .mul(poisson)
        .mul(poisson)
        .write(dst.getChild("poisson2.png"))*/

}