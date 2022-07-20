package me.anno.tests

import me.anno.utils.structures.arrays.LineSequence

fun main() {
    val ls = LineSequence()
    ls.setText(
        "this is great\n" +
                "isn't it?"
    )
    println(ls)
    ls[0, 0] = 'T'.code
    ls[1, 0] = 'I'.code
    ls.insert(1, "isn't it".length, '!'.code)
    println(ls)
    ls.remove(1, "isn't it!".length)
    println(ls)
}