package me.anno.experiments.imagecorrelation

import me.anno.engine.OfficialExtensions
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.raw.FloatImage
import me.anno.utils.Color.b01
import me.anno.utils.OS.downloads

/**
 * a lot faster, but our results show that just correlating the image is not enough:
 * we typically change it too much, and by doing that, the result gets changed a lot, too
 * */
fun main() {
    OfficialExtensions.initForTests()
    val scale = 5
    val full = ImageCache[downloads.getChild("corr_sample_full.jpg")].waitFor()!!.blur(scale)
    println("Image Size: ${full.width} x ${full.height}")
    val part1 = ImageCache[downloads.getChild("corr_sample_cutout.jpg")].waitFor()!!.blur(scale)
    val part2 = ImageCache[downloads.getChild("corr_sample_cutout_cleaned.jpg")].waitFor()!!.blur(scale)
    println("Cutout Size: ${part1.width} x ${part1.height}")
    println(fullCorrelation(full, part1, 0, 0))
    println(borderCorrelation(full, part1, 0, 0))
    println(fullCorrelation(full, part2, 0, 0))
    println(borderCorrelation(full, part2, 0, 0))
    val step = 1
    val correlation = FloatImage((full.width - part2.width) / step, (full.height - part2.height) / step, 1)
    correlation.forEachPixel { x, y ->
        val value = fullCorrelation(full, part1, x * step, y * step)
        correlation.setValue(x, y, 0, value)
    }
    correlation.write(downloads.getChild("corr_result_full-1.jpg"))
    correlation.forEachPixel { x, y ->
        val value = fullCorrelation(full, part2, x * step, y * step)
        correlation.setValue(x, y, 0, value)
    }
    correlation.write(downloads.getChild("corr_result_full-2.jpg"))
    correlation.forEachPixel { x, y ->
        val value = borderCorrelation(full, part1, x * step, y * step)
        correlation.setValue(x, y, 0, value)
    }
    correlation.write(downloads.getChild("corr_result_border-1.jpg"))
    correlation.forEachPixel { x, y ->
        val value = borderCorrelation(full, part2, x * step, y * step)
        correlation.setValue(x, y, 0, value)
    }
    correlation.write(downloads.getChild("corr_result_border-2.jpg"))
}

fun Image.blur(scale: Int): Image {
    val r = r().blur(scale)
    val g = g().blur(scale)
    val b = b().blur(scale)
    return join(r, g, b)
}

fun FloatImage.blur(scale: Int): FloatImage {
    val dst = FloatImage(width / scale, height / scale, 1)
    val invScale = 1f / (scale * scale)
    dst.forEachPixel { x, y ->
        var sum = 0f
        for (dy in 0 until scale) {
            for (dx in 0 until scale) {
                sum += getValue(x * scale + dx, y * scale + dy, 0)
            }
        }
        dst.setValue(x, y, 0, sum * invScale)
    }
    return dst
}

fun join(r: FloatImage, g: FloatImage, b: FloatImage): FloatImage {
    val dst = FloatImage(r.width, r.height, 3)
    dst.forEachPixel { x, y ->
        dst.setValue(x, y, 0, r.getValue(x, y, 0))
        dst.setValue(x, y, 1, g.getValue(x, y, 0))
        dst.setValue(x, y, 2, b.getValue(x, y, 0))
    }
    return dst
}

fun Image.getChannel(shift: Int): FloatImage {
    val dst = FloatImage(width, height, 1)
    dst.forEachPixel { x, y ->
        val value = getRGB(x, y).shr(shift).b01()
        dst.setValue(x, y, 0, value)
    }
    return dst
}

fun Image.r() = getChannel(16)
fun Image.g() = getChannel(8)
fun Image.b() = getChannel(0)
