package me.anno.tests.mesh.dae

import me.anno.Engine
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.downloads

fun main() {
    // Error loading model tmp://13.txt, Collada: $$$___magic___$$$. - Unknown reference format
    OfficialExtensions.initForTests()
    PrefabCache[downloads.getChild("3d/FemaleStandingPose/collada.dae")]!!
    Engine.requestShutdown()
}