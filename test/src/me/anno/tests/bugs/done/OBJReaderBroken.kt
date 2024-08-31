package me.anno.tests.bugs.done

import me.anno.Engine
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.downloads

fun main() {
    // the issue was that I broke TextFileReader.readInt() for negative numbers
    OfficialExtensions.initForTests()
    MeshCache[downloads.getChild("3d/dragon.obj")]
    Engine.requestShutdown()
}