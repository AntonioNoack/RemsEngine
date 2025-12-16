package me.anno.experiments.imagecorrelation

import me.anno.engine.OfficialExtensions
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.raw.FloatImage
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS.downloads
import me.anno.utils.callbacks.I2U

fun main() {
    OfficialExtensions.initForTests()
    // given a big image (with watermark), and a small cut out (with removed watermark),
    //  find the place where it was coming from using correlation
    // do that by scaling down the image to a reasonable size,
    //  and then try all 100 or so combinations and recursively refine the result
    //  -> if our cut-out is small, we kind of have to try lots more than just that...
    // we remove the center, so only match the border, maybe 1-3px wide :)
    val full = ImageCache[downloads.getChild("corr_sample_full.jpg")].waitFor()!!
    println("Image Size: ${full.width} x ${full.height}")
    val part1 = ImageCache[downloads.getChild("corr_sample_cutout.jpg")].waitFor()!!
    val part2 = ImageCache[downloads.getChild("corr_sample_cutout_cleaned.jpg")].waitFor()!!
    println("Cutout Size: ${part1.width} x ${part1.height}")
    println(fullCorrelation(full, part1, 0, 0))
    println(borderCorrelation(full, part1, 0, 0))
    println(fullCorrelation(full, part2, 0, 0))
    println(borderCorrelation(full, part2, 0, 0))
    val step = 3
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

fun fullCorrelation(
    full: Image,
    part: Image,
    x0: Int, y0: Int
): Float {
    val rc = Correlation()
    val gc = Correlation()
    val bc = Correlation()
    part.forEachPixel { x, y ->
        val i = full.getRGB(x + x0, y + y0)
        val t = part.getRGB(x, y)
        rc.push(i.r01(), t.r01())
        gc.push(i.g01(), t.g01())
        bc.push(i.b01(), t.b01())
    }
    return brightness(rc.eval(), gc.eval(), bc.eval())
}

fun Image.forEachBorderPixel(callback: I2U) {
    for (x in 0 until width) {
        callback.call(x, 0)
        callback.call(x, height - 1)
    }
    for (y in 1 until height - 1) {
        callback.call(0, y)
        callback.call(width - 1, y)
    }
}

fun borderCorrelation(
    full: Image,
    part: Image,
    x0: Int, y0: Int
): Float {
    val rc = Correlation()
    val gc = Correlation()
    val bc = Correlation()
    part.forEachBorderPixel { x, y ->
        val i = full.getRGB(x + x0, y + y0)
        val t = part.getRGB(x, y)
        rc.push(i.r01(), t.r01())
        gc.push(i.g01(), t.g01())
        bc.push(i.b01(), t.b01())
    }
    return brightness(rc.eval(), gc.eval(), bc.eval())
}