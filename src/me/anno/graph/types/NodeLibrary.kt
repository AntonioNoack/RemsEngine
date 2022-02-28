package me.anno.graph.types

import me.anno.graph.Node
import me.anno.graph.types.flow.actions.PrintNode
import me.anno.graph.types.flow.control.ForNode
import me.anno.graph.types.flow.control.IfElseNode
import me.anno.graph.types.flow.control.WhileNode
import me.anno.graph.types.flow.local.GetLocalVariableNode
import me.anno.graph.types.flow.local.SetLocalVariableNode
import me.anno.graph.types.flow.maths.MathD2Node
import me.anno.graph.types.flow.maths.MathL2Node

class NodeLibrary(val nodes: Set<Node>) {
    companion object {
        val flowNodes = NodeLibrary(
            hashSetOf(
                GetLocalVariableNode(),
                SetLocalVariableNode(),
                ForNode(),
                IfElseNode(),
                WhileNode(),
                PrintNode(),
                MathL2Node(),
                MathD2Node()
            )
        )
    }
}