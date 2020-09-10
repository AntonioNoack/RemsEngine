package me.anno.ui.editor.cutting

import org.joml.Vector4f

fun main(){

    val g = Gradient(null,
        0, 2,
        Vector4f(0f, 0f, 0f, 0f), Vector4f(0.2f, 0f, 0f, 0.2f)
    )

    println(g.isLinear(5, 1, Vector4f(0.5f, 0f, 0f, 0.5f)))// t
    println(g.isLinear(5, 1, Vector4f(0.4f, 0f, 0f, 0.5f)))// f
    println(g.isLinear(5, 1, Vector4f(0.6f, 0f, 0f, 0.5f)))// f

    println(g.isLinear(5, 2, Vector4f(0.5f, 0f, 0f, 0.5f)))// t
    println(g.isLinear(5, 2, Vector4f(0.4f, 0f, 0f, 0.5f)))// t
    println(g.isLinear(5, 2, Vector4f(0.6f, 0f, 0f, 0.5f)))// t

}

fun t1(){

    val g = Gradient(null,
        0, 1,
        Vector4f(0f), Vector4f(1f)
    )

    println(g.isLinear(5, 1, Vector4f(5f)))// t
    println(g.isLinear(5, 1, Vector4f(4f)))// f
    println(g.isLinear(5, 1, Vector4f(6f)))// f

}