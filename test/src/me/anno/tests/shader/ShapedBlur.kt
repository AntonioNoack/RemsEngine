package me.anno.tests.shader

import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.ShaderLib.gamma
import me.anno.gpu.shader.effects.ShapedBlur.applyFilter
import me.anno.gpu.shader.effects.ShapedBlur.decompress
import me.anno.gpu.shader.effects.ShapedBlur.fileName
import me.anno.gpu.texture.TextureCache
import me.anno.image.raw.FloatImage
import me.anno.io.Streams.write0String
import me.anno.io.Streams.writeLE16
import me.anno.io.Streams.writeLE32
import me.anno.io.files.FileReference
import me.anno.jvm.HiddenOpenGLContext
import me.anno.maths.Maths
import me.anno.maths.Optimization
import me.anno.network.ResetByteArrayOutputStream
import me.anno.sdf.shapes.SDFHeart
import me.anno.utils.OS
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.createList
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Vector2d
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.OutputStream
import java.util.Random
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

fun main() {
    saveFilters()
}

fun saveFilters() {
    val archive1 = OS.downloads.getChild("supplemental_shaders.zip/custom")
    val out = OS.downloads.getChild(fileName).outputStream()
    val tmp = ResetByteArrayOutputStream(2048)
    for (file in archive1.listChildren()) {
        if (file.lcExtension == "glsl" && !file.name.startsWith("2020")) {
            compress(file.readTextSync(), tmp)
            addFilter(file.nameWithoutExtension, out, tmp)
        }
    }
    out.write(0) // marker that gaussian blur is next
    val archive2 = OS.downloads.getChild("supplemental_shaders.zip/gaussians")
    for (file in archive2.listChildren()) {
        if (file.lcExtension == "glsl") {
            compressGaussian(file.readTextSync(), tmp)
            addFilter(file.nameWithoutExtension.replace("gauss", ""), out, tmp)
        }
    }
    out.write(0) // marker that we are done
    out.close()
}

fun addFilter(name: String, out: OutputStream, tmp: ResetByteArrayOutputStream) {
    out.write0String(name)
    out.writeLE16(tmp.size)
    out.write(tmp.buffer, 0, tmp.size)
    tmp.reset()
}

fun compress(
    src: FileReference,
    dst: FileReference = OS.downloads.getChild(src.nameWithoutExtension + ".bin")
): FileReference {
    val all = src.readTextSync()
    val out = dst.outputStream()
    compress(all, out)
    out.close()
    return dst
}

fun singleTest() {
    val archive = OS.downloads.getChild("supplemental_shaders.zip/custom/heart_5x32.glsl")
    val tmp = compress(archive)
    HiddenOpenGLContext.createOpenGL()
    val source = OS.pictures.getChild("blurTest.png")
    val (shader, stages) = decompress(tmp.inputStreamSync())
    val src = TextureCache[source, false]!!
    applyFilter(src, shader, stages, TargetType.Float16x3, 1f, gamma.toFloat())
        .write(OS.desktop.getChild("heart.png"))
}

fun compress(
    all: String,
    out: OutputStream
) {
    val filters = ArrayList<ArrayList<Vector3f>>()
    for (i in 0 until 100) {
        val idx = all.indexOf("case $i:")
        if (idx < 0) continue
        val idx1 = all.indexOf("break;", idx)
        val case = all.substring(all.indexOf(':', idx) + 1, idx1)
        val lines = case.split('\n')
        val filter = ArrayList<Vector3f>()
        filters.add(filter)
        for (line in lines) {
            // gaussian kernels can be compressed by angle + radius + blades
            val idx2 = line.indexOf("vec2(")
            if (idx2 < 0) continue
            val idx3 = line.indexOf(',', idx2 + 5)
            val idx4 = line.indexOf(')', idx3 + 2)
            val idx5 = line.indexOf('*', idx4 + 1)
            val idx6 = line.indexOf(';', idx5 + 1)
            val dx = line.substring(idx2 + 5, idx3).trim().toFloat()
            val dy = line.substring(idx3 + 1, idx4).trim().toFloat()
            val weight = line.substring(idx5 + 1, idx6).trim().toFloat()
            filter.add(Vector3f(dx, -dy, weight))
        }
    }
    out.write(filters.size)
    for (filter in filters) {
        out.write(filter.size)
    }
    for (filter in filters) {
        val bounds = AABBf()
        for (entry in filter) {
            bounds.union(entry)
        }
        val dx = bounds.minX
        val dy = bounds.minY
        val dz = bounds.minZ
        val sx = 255 / bounds.deltaX
        val sy = 255 / bounds.deltaY
        val sz = 255 / Maths.max(bounds.deltaZ, 1e-9f)
        out.writeLE32(dx)
        out.writeLE32(sx)
        out.writeLE32(dy)
        out.writeLE32(sy)
        out.writeLE32(dz)
        out.writeLE32(sz)
        for (entry in filter) {
            out.write(Maths.clamp(((entry.x - dx) * sx).toInt(), 0, 255))
            out.write(Maths.clamp(((entry.y - dy) * sy).toInt(), 0, 255))
            out.write(Maths.clamp(((entry.z - dz) * sz).toInt(), 0, 255))
        }
    }
}

fun compressGaussian(
    all: String,
    out: OutputStream
) {
    val filters = ArrayList<Vector3f>()
    for (i in 0 until 100) {
        val idx = all.indexOf("case $i:")
        if (idx < 0) continue
        val idx1 = all.indexOf("break;", idx)
        val case = all.substring(all.indexOf(':', idx) + 1, idx1)
        val lines = case.split('\n')
        val filter = Vector3f()
        filters.add(filter)
        for (line in lines) {
            // gaussian kernels can be compressed by angle + radius + blades
            val idx2 = line.indexOf("vec2(")
            if (idx2 < 0) continue
            val idx3 = line.indexOf(',', idx2 + 5)
            val idx4 = line.indexOf(')', idx3 + 2)
            val idx5 = line.indexOf('*', idx4 + 1)
            val idx6 = line.indexOf(';', idx5 + 1)
            val dx = line.substring(idx2 + 5, idx3).trim().toFloat()
            val dy = line.substring(idx3 + 1, idx4).trim().toFloat()
            val weight = line.substring(idx5 + 1, idx6).trim().toFloat()
            filter.set(atan2(dy, dx), Maths.length(dx, dy), round(1f / weight))
        }
    }
    out.write(filters.size)
    for (filter in filters) {
        out.writeLE32(filter.x)
        out.writeLE32(filter.y)
        out.write(filter.z.toInt())
    }
}

fun main2() {

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
            dst.setValue(r + dx, r + dy, 0, Maths.clamp(v))
        }
    }
    dst.normalize().write(OS.desktop.getChild("target.png"))

    val rx = Maths.max(dst.width, dst.height).toDouble() * 5.0 / numLayers
    val rnd = Random()
    val kernels = createList(numLayers) { li ->
        val i0 = li * totalSamples / numLayers
        val i1 = (li + 1) * totalSamples / numLayers
        val c = i1 - i0
        createList(c) {
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
            val wi = Maths.max(it[k + 2], 0.0)
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
            if (first || Maths.random() < 1e-3) {
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
            if (first || Maths.random() < 1e-3) {
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


class Sampler(val position: Vector2d, var weight: Double) {
    override fun toString() = "[(${position.x},${position.y})x$weight]"
}
typealias SparseKernel = List<Sampler>

fun error(src: FloatImage, dst: FloatImage): Double {
    val w = Maths.max(src.width, dst.width)
    val h = Maths.max(src.height, dst.height)
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

fun sparseConvolve(kernels: List<SparseKernel>, write: Boolean): FloatImage {
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
        val ex = ceil(Maths.max(bounds.maxX, -bounds.minX)).toInt() * 2
        val ey = ceil(Maths.max(bounds.maxY, -bounds.minY)).toInt() * 2
        val tmp = FloatImage(image.width + ex, image.height + ey, 1)
        // convolve actually
        sparseConvolve(image, tmp, k)
        if (write) {
            sparseConvolve(listOf(k), false).normalize().write(OS.desktop.getChild("img/krn$i.png"))
            tmp.normalize().write(OS.desktop.getChild("img/img$i.png"))
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