package me.anno.tests.image

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.image.ImageCache
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    ImageCache[downloads.getChild("3d/DamagedHelmet.glb/textures/1.jpg")].waitFor()!!.write(desktop.getChild("base.png"))
    ImageCache[downloads.getChild("3d/DamagedHelmet.glb/textures/1.jpg/b.png")].waitFor()!!.write(desktop.getChild("comp.png"))
    Engine.requestShutdown()
}