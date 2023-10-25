package me.anno.tests.sdf

import me.anno.Engine
import me.anno.sdf.shapes.SDFHeart

fun main() {
    val sample = SDFHeart().apply { bound11() }
    sample.createShaderToyScript()
    Engine.requestShutdown()
}