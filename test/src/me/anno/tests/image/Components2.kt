package me.anno.tests.image

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.image.ImageCache
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    ImageCache[downloads.getChild("3d/DamagedHelmet.glb/textures/1.jpg"), false]!!.write(desktop.getChild("base.png"))
    ImageCache[downloads.getChild("3d/DamagedHelmet.glb/textures/1.jpg/b.png"), false]!!.write(desktop.getChild("comp.png"))
    Engine.requestShutdown()
}