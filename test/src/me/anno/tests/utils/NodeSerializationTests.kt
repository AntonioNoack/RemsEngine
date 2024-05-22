package me.anno.tests.utils

import me.anno.graph.visual.local.GetLocalVariableNode
import me.anno.graph.visual.local.SetLocalVariableNode
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.node.NodeOutput
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.saveable.SaveableArray
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.ui.editor.code.codemirror.LanguageStyle
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NodeSerializationTests {

    @Test
    fun testDeserializingNode() {
        registerCustomClass(NodeInput())
        registerCustomClass(NodeOutput())
        registerCustomClass(SetLocalVariableNode("Vector4f"))
        val source = "[{\"class\":\"SetLocalVariableNode\",\"i:*ptr\":1,\"NodeInput[]:inputs\":[3,{\"i:*ptr\":2," +
                "\"NodeOutput[]:others\":[1,{\"i:*ptr\":3,\"NodeInput[]:others\":[1,2]}]},{\"i:*ptr\":4," +
                "\"S:value\":\"var\"},{\"i:*ptr\":5,\"NodeOutput[]:others\":[1,{\"i:*ptr\":6," +
                "\"NodeInput[]:others\":[1,5],\"l:value\":24}],\"l:value\":24}],\"NodeOutput[]:outputs\":" +
                "[2,{\"i:*ptr\":7},{\"i:*ptr\":8,\"l:value\":6}],\"v3d:position\":[1,2,3]}]"
        val node = JsonStringReader.readFirst(source, InvalidRef, SetLocalVariableNode::class, false)
        if (node.key != "var") throw IllegalStateException()
        if (node.value != 24L) throw IllegalStateException("Value is ${node.value}")
        if ((node.clone() as SetLocalVariableNode).key != "var") throw IllegalStateException()
    }

    @Test
    fun testCloningConnectedNodes() {
        registerCustomClass(NodeInput())
        registerCustomClass(NodeOutput())
        registerCustomClass(SetLocalVariableNode())
        val a = SetLocalVariableNode("a", "var")
        val evilHelper = SetLocalVariableNode("b", "bar")
        a.connectTo(evilHelper)
        val listStr = a.toString()
        val clone = JsonStringReader.readFirst(listStr, InvalidRef, SetLocalVariableNode::class, false)
        val cloneStr = clone.toString()
        assertEquals(listStr, cloneStr)
        assertEquals("a", clone.key)
        assertEquals("var", clone.value)
    }

    @Test
    fun testSavingGetLocalVarNode() {
        registerCustomClass(NodeInput())
        registerCustomClass(NodeOutput())
        registerCustomClass(GetLocalVariableNode())
        registerCustomClass(SaveableArray())
        val a = GetLocalVariableNode("a", "?")
        val b = GetLocalVariableNode("b", "?")
        a.connectTo(b)
        val list = SaveableArray(listOf(a, b))
        val listStr = list.toString()
        val clone = JsonStringReader.readFirst(listStr, InvalidRef, SaveableArray::class, false)
        val cloneStr = clone.toString()
        assertEquals(listStr, cloneStr)
    }

    @Test
    fun testSerializeLanguageStyle() { // todo move this elsewhere
        registerCustomClass(LanguageStyle())
        val style1 = LanguageStyle()
        style1.color = 0x775533
        val text = JsonStringWriter.toText(style1, InvalidRef)
        val style2 = JsonStringReader.readFirst(text, InvalidRef, LanguageStyle::class)
        assertEquals(style1.color, style2.color)
    }
}