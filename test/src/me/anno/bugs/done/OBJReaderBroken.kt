package me.anno.bugs.done

import me.anno.Engine
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.downloads

fun main() {
    // the issue was that I broke TextFileReader.readInt() for negative numbers
    OfficialExtensions.initForTests()
    MeshCache.getEntry(downloads.getChild("3d/dragon.obj")).waitFor()
    Engine.requestShutdown()
}