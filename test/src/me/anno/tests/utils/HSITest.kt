package me.anno.tests.utils

import me.anno.ui.editor.color.spaces.LinearHSI
import me.anno.utils.LOGGER
import org.joml.Vector3f
import java.util.*

fun main() {
    val space = LinearHSI
    val random = Random(1234)
    val rgb = Vector3f()
    for (i in 0 until 1000) {
        rgb.x = random.nextFloat()
        rgb.y = random.nextFloat()
        rgb.z = random.nextFloat()
        val internal = space.fromRGB(rgb)
        val rgb2 = space.toRGB(internal)
        val error = rgb.distance(rgb2)
        if (error > 1e-3f) LOGGER.info("$rgb -> $internal -> $rgb2")
    }
}