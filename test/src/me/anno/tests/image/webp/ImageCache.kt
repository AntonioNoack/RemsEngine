package me.anno.tests.image.webp

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.utils.OS.pictures

fun main() {
    OfficialExtensions.initForTests()
    ImageCache[pictures.getChild("Anime/gplus-1899045053.webp"), false]!!
    Engine.requestShutdown()
}