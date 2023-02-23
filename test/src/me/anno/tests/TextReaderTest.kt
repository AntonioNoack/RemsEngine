package me.anno.tests

import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.local.GetLocalVariableNode
import me.anno.graph.types.flow.local.SetLocalVariableNode
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.SaveableArray
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.ui.editor.code.codemirror.LanguageStyle

fun main() {
    NodeLibrary.init()
    registerCustomClass(SaveableArray())
    registerCustomClass(LanguageStyle())
    val source = "[{\"class\":\"SetLocalVariableNode\",\"i:*ptr\":1,\"NodeInput[]:inputs\":[3,{\"i:*ptr\":2," +
            "\"NodeOutput[]:others\":[1,{\"i:*ptr\":3,\"NodeInput[]:others\":[1,2]}]},{\"i:*ptr\":4," +
            "\"S:value\":\"var\"},{\"i:*ptr\":5,\"NodeOutput[]:others\":[1,{\"i:*ptr\":6," +
            "\"NodeInput[]:others\":[1,5],\"l:value\":24}],\"l:value\":24}],\"NodeOutput[]:outputs\":" +
            "[2,{\"i:*ptr\":7},{\"i:*ptr\":8,\"l:value\":6}],\"v3d:position\":[1,2,3]}]\n"
    val node = TextReader.readFirst<SetLocalVariableNode>(source, InvalidRef, false)
    if (node.key != "var") throw IllegalStateException()
    if (node.value != 24L) throw IllegalStateException("Value is ${node.value}")
    if ((node.clone() as SetLocalVariableNode).key != "var") throw IllegalStateException()
    test5()
    test3()
    test4()
}

fun test5() {
    val a = SetLocalVariableNode("a", "var")
    val evilHelper = SetLocalVariableNode("b", "bar")
    a.connectTo(evilHelper)
    val listStr = a.toString()
    val clone = TextReader.readFirst<SetLocalVariableNode>(listStr, InvalidRef, false)
    val cloneStr = clone.toString()
    if (listStr == cloneStr && clone.key == "a" && clone.value == "var") {
        println("Same contents :)")
        println(listStr)
    } else {
        println(listStr)
        println(cloneStr)
        if (clone.key != "a") throw IllegalStateException("Incorrect Key: ${clone.key} != 'a'")
        if (clone.value != "var") throw IllegalStateException("Incorrect Value: ${clone.value} != 'var'")
        throw IllegalStateException("Content mismatch!")
    }
}

fun test4() {
    val a = GetLocalVariableNode("a", "?")
    val b = GetLocalVariableNode("b", "?")
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
    val style1 = LanguageStyle()
    style1.color = 0x775533
    val text = TextWriter.toText(style1, InvalidRef)
    val style2 = TextReader.readFirst<LanguageStyle>(text, InvalidRef)
    if (style2.color != style1.color) throw RuntimeException("$text -> " + style2.color.toString(16))
}