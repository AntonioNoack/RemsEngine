package me.anno.tests.image

import me.anno.config.DefaultConfig.style
import me.anno.gpu.shader.ShaderLib
import me.anno.image.Image
import me.anno.image.ImageCPUCache
import me.anno.image.ImageGPUCache
import me.anno.image.raw.IntImage
import me.anno.ui.base.groups.ZoomableImagePanel
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.hex24
import me.anno.utils.Color.hex4
import me.anno.utils.Color.r01
import me.anno.utils.Color.rgba
import me.anno.utils.OS.desktop
import me.anno.video.formats.cpu.ARGBFrame.rgb2yuv
import me.anno.video.formats.cpu.ARGBFrame.yuv2rgb
import org.joml.AABBf
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector3f
import java.util.*

/**
 * CCTweaked is a Minecraft mod like ComputerCraft.
 * The display is pretty limited, supporting only 15 colors.
 * This function tries to extract the most important colors using clustering (no dithering yet),
 * and prints them in a format that is already supported there, plus the palette for a small, custom program.
 *
 * Not a good palettizing algorithm! Use Gimp for better results!
 * */
fun main() {

    testYUV2RGB()

    val maxNumClusters = 15
    val useYUV = true
    val image = ImageCPUCache[desktop.getChild("seal.png"), false]!!.createIntImage()

    if (useYUV) {
        val data = image.data
        for (i in data.indices) {
            data[i] = rgb2yuv(data[i])
        }
    }

    val uniqueColors = HashMap<Int, Int>()
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val color = image.getRGB(x, y) and 0xffffff
            uniqueColors[color] = (uniqueColors[color] ?: 0) + 1
        }
    }

    val palette: IntArray
    val newPixels: IntArray
    if (uniqueColors.size <= maxNumClusters) {
        val entries = uniqueColors.keys.mapIndexed { index, color -> color to index }.toList()
        val idxToColor = entries.associate { it.second to it.first }
        palette = IntArray(uniqueColors.size) { idxToColor[it]!! }
        val colorToIdx = entries.associate { it }
        newPixels = IntArray(image.width * image.height) {
            val color = image.getRGB(it) and 0xffffff
            colorToIdx[color]!!
        }
    } else {

        class Cluster(val index: Int) {
            val value = Vector3f()
            val sum = Vector3f()
            var weight = 0f
            fun distance(r: Float, g: Float, b: Float): Float {
                return value.distanceSquared(r, g, b)
            }
        }

        val clusters = Array(maxNumClusters) { Cluster(it) }
        val mostCommonColors = uniqueColors.entries.sortedByDescending { it.value }
        for ((index, entry) in mostCommonColors.withIndex()) {
            if (index >= maxNumClusters) break
            val (color, _) = entry
            clusters[index].value.set(color.r01(), color.g01(), color.b01())
        }

        fun findBestI(r: Float, g: Float, b: Float): Int {
            var bestD = clusters[0].distance(r, g, b)
            var bestI = 0
            for (i in 1 until clusters.size) {
                val dist = clusters[i].distance(r, g, b)
                if (dist < bestD) {
                    bestD = dist
                    bestI = i
                }
            }
            return bestI
        }

        fun findBestI(color: Int): Int {
            val r = color.r01()
            val g = color.g01()
            val b = color.b01()
            return findBestI(r, g, b)
        }

        // cluster algorithm
        val numIter = 50
        for (iter in 0 until numIter) {
            // assign colors to the closest cluster
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val color = image.getRGB(x, y) and 0xffffff
                    val r = color.r01()
                    val g = color.g01()
                    val b = color.b01()
                    val best = clusters[findBestI(r, g, b)]
                    val w = 1f
                    best.sum.add(r * w, g * w, b * w)
                    best.weight += w
                }
            }
            // center clusters
            // reset state
            for (cluster in clusters) {
                if (cluster.weight > 0f) {
                    cluster.sum.div(cluster.weight)
                }
                if (iter < numIter - 1) { // not the last round
                    cluster.sum.set(0f)
                    cluster.weight = 0f
                }
            }
        }
        // sort by most common color (weight) :)
        val byWeight = clusters
            .filter { it.weight > 0f }
            .sortedByDescending { it.weight }
        val clusterIdxToId = IntArray(byWeight.size)
        palette = IntArray(maxNumClusters) { idx ->
            val cluster = byWeight[idx]
            clusterIdxToId[cluster.index] = idx
            rgba(cluster.value.x, cluster.value.y, cluster.value.z, 1f)
        }
        newPixels = IntArray(image.width * image.height) {
            val color = image.getRGB(it) and 0xffffff
            val bestI = findBestI(color)
            clusterIdxToId[bestI]
        }
    }

    if (useYUV) {
        for (i in palette.indices) {
            palette[i] = yuv2rgb(palette[i])
        }
    }

    val image1 = IntImage(image.width, image.height, newPixels, false)
    printResult(palette, image1)
    preview(palette, image1)
}

fun testYUV2RGB() {
    val rnd = Random()
    var maxDist = 0f
    val bdx = AABBf()
    for (i in 0 until 1_000_000) {
        val rgbIn = rnd.nextInt() and 0xffffff
        val yuv = rgb2yuv(rgbIn)
        val rgbOut = yuv2rgb(yuv)
        val dist = Vector3f.length(
            rgbIn.r01() - rgbOut.r01(),
            rgbIn.g01() - rgbOut.g01(),
            rgbIn.b01() - rgbOut.b01()
        )
        bdx.union(yuv.r01(), yuv.g01(), yuv.b01())
        if (dist > maxDist) {
            println("Dist[$i] $dist: ${hex24(rgbIn)} -> ${hex24(rgbOut)} via ${hex24(yuv)}")
            maxDist = dist
        }
    }
    println(maxDist)
}

fun printResult(palette: IntArray, image: IntImage) {
    print("Palette: ")
    for (color in palette) {
        print(hex24(color))
    }
    println()
    println("Image:")
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            print(hex4(image.getRGB(x, y) + 1))
        }
        println()
    }
}

fun preview(palette: IntArray, image: IntImage) {
    applyPalette(palette, image)
    preview(image)
}

fun applyPalette(palette: IntArray, image: IntImage) {
    val data = image.data
    for (i in data.indices) {
        data[i] = palette[data[i]]
    }
}

fun preview(image: Image) {
    // to do show palette, too?
    testUI3("CCTweaked") {
        object : ZoomableImagePanel(style) {
            override fun getTexture() = ImageGPUCache[image.ref, true]
        }
    }
}