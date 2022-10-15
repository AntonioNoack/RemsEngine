package me.anno.tests

import me.anno.ui.editor.color.spaces.LinearHSI
import me.anno.utils.LOGGER
import me.anno.utils.types.Vectors.print
import org.joml.Vector3f
import java.util.*

fun main(){
    val space = LinearHSI
    val random = Random(1234)
    val rgb = Vector3f()
    for(i in 0 until 1000){
        rgb.x = random.nextFloat()
        rgb.y = random.nextFloat()
        rgb.z = random.nextFloat()
        val internal = LinearHSI.fromRGB(rgb)
        val rgb2 = LinearHSI.toRGB(internal)
        val error = rgb.distance(rgb2)
        if(error > 1e-3f) LOGGER.info("${rgb.print()} -> ${internal.print()} -> ${rgb2.print()}")
    }
}