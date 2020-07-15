package me.anno.utils.test

import me.anno.utils.print
import org.joml.Matrix4f
import org.joml.Vector4f

fun main(){

    val m = Matrix4f().perspective(1f, 1f, 0.5f, 10f)

    println(m)

    println(m.transformProject(Vector4f(0f, 0f, -1f, 1f)).print())

    println(m.transform(Vector4f(0f, 0f, -1f, 1f)).print())



}