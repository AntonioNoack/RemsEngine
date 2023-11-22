package me.anno.tests.image.segmentation

import me.anno.image.ImageCache
import me.anno.image.raw.ByteImage
import me.anno.image.raw.IntImage
import me.anno.maths.Maths
import me.anno.utils.Color.a
import me.anno.utils.OS
import me.anno.utils.types.Floats.formatPercent
import me.anno.video.formats.cpu.ARGBFrame.rgb2yuv
import me.anno.video.formats.cpu.ARGBFrame.yuv2rgb
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

// todo use normal color clustering...

fun main(){

    val maxError = 0.1f
    val image = ImageCache[OS.pictures.getChild("Anime/img (3).webp"), false]!!
        .createIntImage()

    val transformIntoYuv = false // true makes things even worse, idk why...

    val imageData = image.data
    if (transformIntoYuv) for (i in imageData.indices) {
        imageData[i] = rgb2yuv(imageData[i])
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

    val dxs = intArrayOf(-1, +1, 0, 0)
    val dys = intArrayOf(0, 0, -1, +1)
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
                if (min(cluster.count, cluster2.count) * 30 < max(cluster.count, cluster2.count))
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

                if (error <= maxError * Maths.sq(1f + 0.5f * log2(newCluster.count.toFloat()))) {
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
                predicted.setRGB(x, y, yuv2rgb(cluster.predict(x - cluster.cx, y - cluster.cy)))
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
    predicted.write(OS.desktop.getChild("predicted.png"))

    // show predicted image
    val randomColors = ByteArray(numClusters) { if (it == 0) 0 else ((it % 230) + (255 - 230)).toByte() }
    val ids = ByteImage(w, h, ByteImage.Format.R)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val ii = x + y * w
            ids.data[ii] = randomColors[clusterIds[ii]]
        }
    }
    ids.write(OS.desktop.getChild("clusterIds.png"))
}