package me.anno.tests

import me.anno.ecs.prefab.ChangeHistory
import me.anno.io.ISaveable
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter


/**
 * a test for StringHistories compression capabilities
 * */
fun main() {

    ISaveable.registerCustomClass(ChangeHistory())

    val hist = ChangeHistory()
    hist.put("hallo")
    hist.put("hello")
    hist.put("hello world")
    hist.put("hell")
    hist.put("hello world, you")
    hist.put("kiss the world")
    hist.put("this is the world")
    hist.put("that was le world")

    val str = TextWriter.toText(hist, InvalidRef)
    println(str)

    val hist2 = TextReader.readFirstOrNull<ChangeHistory>(str, InvalidRef)!!

    val str2 = TextWriter.toText(hist2, InvalidRef)

    if (str != str2) {
        println(str2)
        throw RuntimeException()
    }

}