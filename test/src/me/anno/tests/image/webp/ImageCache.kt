package me.anno.tests.image.webp

import me.anno.image.ImageCache
import me.anno.utils.OS.pictures

fun main() {
    ImageCache[pictures.getChild("Anime/gplus-1899045053.webp"), false]!!
}