package me.anno.tests.files

import me.anno.io.files.FileReference
import me.anno.io.json.JsonFormatter
import me.anno.io.numpy.NumPyReader
import me.anno.utils.structures.tuples.Quad

fun main() {
    val src = FileReference.getReference("C:/Users/Antonio/Documents/MakeHuman/data/clothes/fedora01/fedora.npz")
    val data = NumPyReader.readNPZ(src)
    println(
        JsonFormatter.format(data.mapValues { (_, v) ->
            if (v is Quad<*, *, *, *>) listOf(v.a, v.b, v.c, v.d)
            else v
        }, "\t", 500)
    )
}
