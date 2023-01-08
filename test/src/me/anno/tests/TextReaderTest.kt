package me.anno.tests

import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.local.GetLocalVariableNode
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.SaveableArray
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.ui.editor.code.codemirror.LanguageStyle

fun main() {
    NodeLibrary.init()
    registerCustomClass(SaveableArray())
    val a = GetLocalVariableNode("a")
    val b = GetLocalVariableNode("b")
    a.connectTo(b)
    val list = SaveableArray(listOf(a, b))
    val listStr = list.toString()
    val clone = TextReader.readFirst<SaveableArray>(listStr, InvalidRef, false)
    val cloneStr = clone.toString()
    if (listStr == cloneStr) {
        println("Same contents :)")
        println(listStr)
    } else {
        println(listStr)
        println(cloneStr)
        if (list.size != clone.size)
            throw IllegalStateException("Size mismatch! ${list.size} vs ${clone.size}")
        else throw IllegalStateException("Content mismatch!")
    }
    test3()
}

fun test3() {
    registerCustomClass(LanguageStyle())
    val style1 = LanguageStyle()
    style1.color = 0x775533
    val text = TextWriter.toText(style1, InvalidRef)
    val style2 = TextReader.readFirst<LanguageStyle>(text, InvalidRef)
    if (style2.color != style1.color) throw RuntimeException("$text -> " + style2.color.toString(16))
}