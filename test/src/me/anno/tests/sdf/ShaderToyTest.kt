package me.anno.tests.sdf

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.sdf.shapes.SDFHeart

fun main() {
    OfficialExtensions.initForTests()
    val sample = SDFHeart().apply { bound11() }
    sample.createShaderToyScript()
    Engine.requestShutdown()
}