package me.anno.tests.bench.sdftexture

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.fonts.Font
import me.anno.fonts.signeddistfields.algorithm.SignedDistanceField.computeDistances
import me.anno.utils.Clock
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager

// todo make SDF textures work on the ascii-characters

// todo optimize SDF texture generation
//  1) make it faster by reducing done work and reducing branch misses
//  2) run it on the GPU
fun main() {

    LogManager.disableInfoLogs("Saveable,ExtensionManager,DefaultConfig")
    OfficialExtensions.initForTests()
    val clock = Clock("SDFTextureBench")
    val font = Font("Verdana", 50f)


    val slowestChar = '@'

    fun generate(cp: Int) {
        val text = listOf(cp).joinChars()
        val roundEdges = false
        computeDistances(font, text, roundEdges)
    }

    clock.benchmark(1, 5, "SDFChars[${slowestChar}]") {
        generate(slowestChar.code)
    }

    Engine.requestShutdown()
}