package me.anno.tests.image.segmentation

import me.anno.image.ImageCache
import me.anno.image.raw.ByteImage
import me.anno.image.raw.IntImage
import me.anno.maths.LinearRegression
import me.anno.maths.Maths.sq
import me.anno.utils.Color.a
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.Color.rgba
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.structures.arrays.DoubleArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Floats.formatPercent
import me.anno.video.formats.cpu.YUVFrames
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val matrix = Array(4) { DoubleArrayList(100) }
val vector = Array(4) { DoubleArrayList(100) }

var polyBetter = 0
var avgBetter = 0

class Formula(val idx: Int) {

    val params = FloatArray(6) // a + b*x + c*y + d*x*y + e*x² + f*y²

    fun add(dx: Int, dy: Int, v: Float) {
        val matrix = matrix[idx]
        matrix.add(1.0)
        matrix.add(dx.toDouble())
        matrix.add(dy.toDouble())
        matrix.add((dx * dy).toDouble())
        matrix.add((dx * dx).toDouble())
        matrix.add((dy * dy).toDouble())
        vector[idx].add(v.toDouble())
    }

    fun reset() {
        params.fill(0f)
        matrix[idx].clear()
        vector[idx].clear()
    }

    fun predict(dx: Float, dy: Float): Float {
        return params[0] + params[1] * dx + params[2] * dy +
                params[3] * dx * dy + params[4] * dx * dx + params[5] * dy * dy
    }

    fun solve() {

        val matrix = matrix[idx]
        val vector = vector[idx]

        if (vector.size == 1) {
            params[0] = vector[0].toFloat()
        } else {
            params.fill(0f)
            val answer = LinearRegression.solve(matrix.toArray(), vector.toArray(), 1e-6)
            if (answer != null) {
                for (i in 0 until 6) {
                    params[i] = answer[i].toFloat()
                }
            }
            var avgColor = 0.0
            for (i in 0 until vector.size) {
                avgColor += vector[i]
            }
            avgColor /= vector.size
            var e0 = 0.0 // simple error by avg
            var e1 = 0.0 // complex error using polynomial, always should be better
            for (i in 0 until vector.size) {
                val j = i * 6
                e0 = max(e0, sq(vector[i] - avgColor))
                e1 = max(e1, sq(vector[i] - predict(matrix[j + 1].toFloat(), matrix[j + 2].toFloat())))
            }
            // polynomial was wrong
            if (e0 >= e1) {
                polyBetter++
            } else {
                avgBetter++
                params.fill(0f)
                params[0] = avgColor.toFloat()
            }
        }
    }
}

class Cluster(val id: Int) {

    val f0 = Formula(0)
    val f1 = Formula(1)
    val f2 = Formula(2)
    val f3 = Formula(3)

    var alive = true

    val neighbors = HashSet<Cluster>(4)

    var cx = 0
    var cy = 0

    var sdx = 0
    var sdy = 0
    var count = 0
    var adx = 0
    var ady = 0
    var adxy = 0

    var error = 0f

    fun add(dx: Int, dy: Int, rgb: Int) {
        adx += abs(dx)
        ady += abs(dy)
        adxy += abs(dx * dy)
        count++
        sdx += dx
        sdy += dy
        // pre-multiplied colors for easier error estimation
        val a = rgb.a01()
        val r = rgb.r01() * a
        val g = rgb.g01() * a
        val b = rgb.b01() * a
        f0.add(dx, dy, a)
        f1.add(dx, dy, r)
        f2.add(dx, dy, g)
        f3.add(dx, dy, b)
    }

    fun solve() {
        f0.solve()
        f1.solve()
        f2.solve()
        f3.solve()
    }

    fun getError(dx: Int, dy: Int, rgb: Int): Float {
        return getError(dx.toFloat(), dy.toFloat(), rgb)
    }

    fun getError(dx: Float, dy: Float, rgb: Int): Float {
        val a = rgb.a01()
        val r = rgb.r01() * a
        val g = rgb.g01() * a
        val b = rgb.b01() * a
        val ea = f0.predict(dx, dy) - a
        val er = f1.predict(dx, dy) - r
        val eg = f2.predict(dx, dy) - g
        val eb = f3.predict(dx, dy) - b
        return ea * ea + er * er + eg * eg + eb * eb
    }

    fun predict(dx: Int, dy: Int): Int {
        return predict(dx.toFloat(), dy.toFloat())
    }

    fun predict(dx: Float, dy: Float): Int {
        val a = f0.predict(dx, dy)
        val inv = 1f / max(a, 1e-7f)
        val r = f1.predict(dx, dy) * inv
        val g = f2.predict(dx, dy) * inv
        val b = f3.predict(dx, dy) * inv
        return rgba(r, g, b, a)
    }

    fun reset() {
        neighbors.clear()
        sdx = 0
        sdy = 0
        count = 0
        f0.reset()
        f1.reset()
        f2.reset()
        f3.reset()
    }

    fun moveMaybe(clusterIds: IntArray, w: Int, h: Int, ci: Int) {
        if (count > 0) {
            val ncx = cx + (sdx.toFloat() / count).roundToInt()
            val ncy = cy + (sdy.toFloat() / count).roundToInt()
            if (ncx in 0 until w && ncy in 0 until h &&
                clusterIds[ncx + ncy * w] == ci
            ) {
                cx = ncx
                cy = ncy
                sdx = 0
                sdy = 0
            }
        }
    }
}

val todo = IntArrayList(4096)
fun check(x: Int, y: Int, call: (x: Int, y: Int, callback: (Int, Int) -> Unit) -> Unit) {
    val todo = todo
    todo.clear()
    todo.add(x)
    todo.add(y)
    val callback = { xi: Int, yi: Int ->
        todo.add(xi)
        todo.add(yi)
    }
    while (todo.isNotEmpty()) {
        val idx = todo.size - 2
        val xi = todo[idx]
        val yi = todo[idx + 1]
        todo.size = idx
        call(xi, yi, callback)
    }
}

fun main() {

    // load image

    val maxError = 0.01f
    val image = ImageCache[downloads.getChild("lib/rem sakamileo deviantart.png"), false]!!
        .createIntImage()

    val transformIntoYuv = false // true makes things even worse, idk why...

    val imageData = image.data
    if (transformIntoYuv) for (i in imageData.indices) {
        imageData[i] = YUVFrames.rgb2yuv(imageData[i])
    }

    val w = image.width
    val h = image.height

    // 0 = none
    val clusterIds = IntArray(w * h)

    // average initial cluster size
    val cdx = 5
    val cdy = 5

    val clustersX = max(1, w / cdx)
    val clustersY = max(1, h / cdy)

    val numClusters = clustersX * clustersY + 1

    val clusters = Array(numClusters) { Cluster(it) }
    val clusterIdMapping = IntArray(numClusters - 1) { it }
    clusterIdMapping.shuffle()

    // create initial cluster centers
    for (y in 0 until clustersY) {
        val yi = y * h / clustersY
        for (x in 0 until clustersX) {
            val xi = x * w / clustersX
            val ci = clusterIdMapping[x + y * clustersX] + 1
            val cluster = clusters[ci]
            cluster.cx = xi
            cluster.cy = yi
            val j = xi + yi * w
            cluster.alive = imageData[j].a() > 63
            if (cluster.alive) clusterIds[j] = ci
        }
    }

    val dxs = intArrayOf(0, -1, +1, 0, 0)
    val dys = intArrayOf(0, 0, 0, -1, +1)
    val errors = FloatArray(w * h * 5) // 4 for each main direction (-x,+x,-y,+y)
    val checkedPixels = IntArray(w * h)
    val newCluster = Cluster(-1)
    for (k in 0 until 250) {

        println("step $k, polynomial was better in ${(polyBetter.toFloat() / avgBetter).formatPercent()}% of cases")
        avgBetter = 0
        polyBetter = 0

        checkedPixels.fill(0)
        // define clusters and their pattern :)
        var ki = 0
        for (ci in 1 until numClusters) {
            val cluster = clusters[ci]
            if (!cluster.alive) continue
            val cx = cluster.cx
            val cy = cluster.cy
            val cix = cx + cy * w
            if (clusterIds[cix] != ci) {
                cluster.alive = false
                continue
            }
            // calculate avg position
            // calculate pattern
            cluster.moveMaybe(clusterIds, w, h, ci)
            cluster.reset()
            ki++
            check(cx, cy) { x, y, c ->
                val i = x + y * w
                val cid = clusterIds[i]
                if (cid == ci) {
                    if (checkedPixels[i] != ki) {
                        checkedPixels[i] = ki
                        // add pixel to statistics
                        cluster.add(x - cx, y - cy, imageData[i])
                        if (x > 0) c(x - 1, y)
                        if (x < w - 1) c(x + 1, y)
                        if (y > 0) c(x, y - 1)
                        if (y < h - 1) c(x, y + 1)
                    }
                } else if (cid != 0) {
                    cluster.neighbors.add(clusters[cid])
                }
            }
            cluster.solve()

            var error = 0f
            ki++
            check(cx, cy) { x, y, c ->
                val i = x + y * w
                if (clusterIds[i] == ci && checkedPixels[i] != ki) {
                    checkedPixels[i] = ki
                    // add pixel to statistics
                    error = max(error, cluster.getError(x - cx, y - cy, imageData[i]))
                    if (x > 0) c(x - 1, y)
                    if (x < w - 1) c(x + 1, y)
                    if (y > 0) c(x, y - 1)
                    if (y < h - 1) c(x, y + 1)
                }
            }
            cluster.error = error
        }

        // calculate errors
        errors.fill(Float.POSITIVE_INFINITY)
        for (d in dxs.indices) {
            val dx = dxs[d]
            val dy = dys[d]
            val di = dx + dy * w
            val bi = d * w * h
            for (y in max(0, -dy) until h - max(0, dy)) {
                for (x in max(0, -dx) until w - max(0, dx)) {
                    val ii = x + y * w
                    val other = clusterIds[ii + di]
                    if (other == 0) continue
                    val cluster = clusters[other]
                    val color = imageData[ii]
                    if (color.a() < 5) continue
                    errors[bi + ii] = cluster.getError(x - cluster.cx, y - cluster.cy, color)
                }
            }
        }

        // add pixels to cluster, if equation matches well
        // -> find pixels with the lowest error next to nearby cluster
        for (idx in errors.indices) {
            if (errors[idx] <= maxError) {
                // find direction with the smallest error
                val x = idx % w
                val y = (idx / w) % h
                var minDir = idx / (w * h)
                var minError = errors[idx]
                val ii = x + y * w
                for (d in dxs.indices) {
                    val bi = d * w * h
                    val err = errors[bi + ii]
                    if (err < minError) {
                        minDir = d
                        minError = err
                    }
                }
                clusterIds[ii] = clusterIds[ii + dxs[minDir] + dys[minDir] * w]
                for (d2 in dxs.indices) {// mark as done
                    val bi = d2 * w * h
                    errors[bi + ii] = Float.POSITIVE_INFINITY
                }
            }
        }

        // migrate pixels from one cluster to another, if that matches them better
        // done by dx=0,dy=0

        // merge clusters, if pattern matches really well :)
        var merged = false
        cis@ for (ci in 1 until numClusters) {
            val cluster = clusters[ci]
            if (!cluster.alive) continue
            val neighbors = cluster.neighbors
            for (cluster2 in neighbors) {
                val ni = cluster2.id
                if (ni <= ci) continue

                // prevents O(n²) behaviour, and might improve result slightly
                if (min(cluster.count, cluster2.count) * 10 < max(cluster.count, cluster2.count))
                    continue

                newCluster.reset()

                fun merge(cluster: Cluster, cluster2: Cluster, error: Float) {
                    if (cluster.count < cluster2.count) {
                        merge(cluster2, cluster, error)
                        return
                    }
                    println("merging $ci (${cluster.count}) and $ni (${cluster2.count}) for a total of ${newCluster.count} pixels")
                    cluster2.alive = false
                    ki++
                    check(cluster2.cx, cluster2.cy) { x, y, c ->
                        val i = x + y * w
                        if (clusterIds[i] == ni && checkedPixels[i] != ki) {
                            checkedPixels[i] = ki
                            clusterIds[i] = ci
                            if (x > 0) c(x - 1, y)
                            if (x < w - 1) c(x + 1, y)
                            if (y > 0) c(x, y - 1)
                            if (y < h - 1) c(x, y + 1)
                        }
                    }
                    if (newCluster.count == cluster.count + cluster2.count) {
                        cluster.count = newCluster.count
                        cluster.sdx = newCluster.sdx
                        cluster.sdy = newCluster.sdy
                        cluster.moveMaybe(clusterIds, w, h, ci)
                    }
                    cluster.error = error
                    merged = true
                }

                if (cluster.f0.params.contentEquals(cluster2.f0.params) &&
                    cluster.f1.params.contentEquals(cluster2.f1.params) &&
                    cluster.f2.params.contentEquals(cluster2.f2.params) &&
                    cluster.f3.params.contentEquals(cluster2.f3.params)
                ) {
                    merge(cluster, cluster2, max(cluster.error, cluster2.error))
                    continue
                }

                val cx = cluster.cx
                val cy = cluster.cy

                // check how well their pattern matches
                // if it does, merge them

                newCluster.cx = cx
                newCluster.cy = cy

                // calculate merged pattern
                ki++
                check(cx, cy) { x, y, c ->
                    val i = x + y * w
                    val cid = clusterIds[i]
                    if (cid == ci || cid == ni) {
                        if (checkedPixels[i] != ki) {
                            checkedPixels[i] = ki
                            // add pixel to statistics
                            newCluster.add(x - cx, y - cy, imageData[i])
                            if (x > 0) c(x - 1, y)
                            if (x < w - 1) c(x + 1, y)
                            if (y > 0) c(x, y - 1)
                            if (y < h - 1) c(x, y + 1)
                        }
                    }
                }
                if (newCluster.count == 0) continue
                newCluster.solve()

                var error = 0f
                ki++
                check(cx, cy) { x, y, c ->
                    val i = x + y * w
                    val cid = clusterIds[i]
                    if ((cid == ci || cid == ni) && checkedPixels[i] != ki) {
                        checkedPixels[i] = ki
                        // add pixel to statistics
                        error = max(error, newCluster.getError(x - cx, y - cy, imageData[i]))
                        if (x > 0) c(x - 1, y)
                        if (x < w - 1) c(x + 1, y)
                        if (y > 0) c(x, y - 1)
                        if (y < h - 1) c(x, y + 1)
                    }
                }

                if (error <= maxError * sq(1f + log2(newCluster.count.toFloat()))) {
                    merge(cluster, cluster2, error)
                    continue
                }// else println("${cluster.error} + ${clusters[ni].error} < $error / 1.1")
            }
        }
        if (!merged && k > max(cdx, cdy)) break
    }

    // show predicted image
    val predicted = IntImage(w, h, true)
    if (transformIntoYuv) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val ii = x + y * w
                val ci = clusterIds[ii]
                if (ci == 0) continue
                val cluster = clusters[ci]
                predicted.setRGB(x, y, YUVFrames.yuv2rgb(cluster.predict(x - cluster.cx, y - cluster.cy)))
            }
        }
    } else {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val ii = x + y * w
                val ci = clusterIds[ii]
                if (ci == 0) continue
                val cluster = clusters[ci]
                predicted.setRGB(x, y, cluster.predict(x - cluster.cx, y - cluster.cy))
            }
        }
    }
    predicted.write(desktop.getChild("predicted.png"))

    // show predicted image
    val randomColors = ByteArray(numClusters) { if (it == 0) 0 else ((it % 230) + (255 - 230)).toByte() }
    val ids = ByteImage(w, h, ByteImage.Format.R)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val ii = x + y * w
            ids.data[ii] = randomColors[clusterIds[ii]]
        }
    }
    ids.write(desktop.getChild("clusterIds.png"))

    // todo split cluster if difference is too large

    // todo transform into better color space, e.g. yuv
}