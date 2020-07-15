package me.anno.utils.test

import me.anno.utils.Clipping
import me.anno.utils.generatePermutations
import org.joml.Vector4f
import org.joml.Vector4fc

fun main(){

    println(
        Clipping.check(
            Vector4f(10f, 0f, 0f, 0f),
            Vector4f(-1f, 0f, 0f, 0f),
            Vector4f(5f, 0f, 0f, 0f)
        ){ it.x }
    )

    val v00 = Vector4f(+1f, +1f, 1f, 1f)
    val v01 = Vector4f(+1f, -1f, 10f, 1f)
    val v10 = Vector4f(-1f, +1f, 3f, 1f)
    val v11 = Vector4f(-1f, -1f, 14f, 1f)

    /*generatePermutations(listOf(v00, v01, v10, v11)){ perm ->
        // check that we don't input a non planar form...
        println(perm.map { it.x })
        println(Clipping.getZ(perm[0], perm[1], perm[2], perm[3]))
    }*/

    println(Clipping.getZ(v00, v01, v10, v11))

}