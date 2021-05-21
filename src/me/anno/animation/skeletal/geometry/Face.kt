package me.anno.animation.skeletal.geometry

import java.lang.RuntimeException

class Face(val a: Int, val b: Int, val c: Int){
    init {
        val isUseless = a == b || b == c || a == c
        if(isUseless) throw RuntimeException("Face is useless: $a $b $c")
    }
}