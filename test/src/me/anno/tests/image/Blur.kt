package me.anno.tests.image

import me.anno.image.utils.BoxBlur.boxBlurX
import me.anno.image.utils.BoxBlur.boxBlurY
import me.anno.image.utils.BoxBlur.multiply
import me.anno.image.ImageCache
import me.anno.image.raw.FloatImage
import me.anno.utils.Color.g01
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures

fun main() {
    val image = ImageCache[pictures.getChild("Anime/90940211_p0_master1200.jpg"), false]!!
    val w = image.width
    val h = image.height
    val fi1 = FloatImage(w, h, 1)
    val fid = fi1.data
    for (i in 0 until w * h) {
        fid[i] = image.getRGB(i).g01()
    }
    val fi2 = fi1.clone()
    fi1.write(desktop.getChild("raw.png"))
    boxBlurY(fid, w, h, 0, w, 20, false)
    boxBlurY(fid, w, h, 0, w, 20, false)
    boxBlurY(fid, w, h, 0, w, 20, false)
    multiply(fid, w, h, 0, w, 1f / (20 * 20 * 20))
    fi1.write(desktop.getChild("blurY.png"))
    boxBlurX(fid, w, h, 0, w, 20, false)
    boxBlurX(fid, w, h, 0, w, 20, false)
    boxBlurX(fid, w, h, 0, w, 20, false)
    multiply(fid, w, h, 0, w, 1f / (20 * 20 * 20))
    fi1.write(desktop.getChild("blurYX.png"))
    boxBlurX(fi2.data, w, h, 0, w, 20, false)
    boxBlurX(fi2.data, w, h, 0, w, 20, false)
    boxBlurX(fi2.data, w, h, 0, w, 20, false)
    multiply(fi2.data, w, h, 0, w, 1f / (20 * 20 * 20))
    fi2.write(desktop.getChild("blurX.png"))
}