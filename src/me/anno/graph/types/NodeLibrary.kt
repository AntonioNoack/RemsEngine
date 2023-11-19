package me.anno.graph.types

import me.anno.ecs.components.anim.graph.AnimStateNode
import me.anno.graph.EnumNode
import me.anno.graph.Node
import me.anno.graph.NodeInput
import me.anno.graph.NodeOutput
import me.anno.graph.render.ColorNode
import me.anno.graph.render.GameTime
import me.anno.graph.render.RandomNode
import me.anno.graph.types.flow.actions.PrintNode
import me.anno.graph.types.flow.config.ConfigGetBoolNode
import me.anno.graph.types.flow.config.ConfigGetFloatNode
import me.anno.graph.types.flow.config.ConfigGetIntNode
import me.anno.graph.types.flow.control.DoWhileNode
import me.anno.graph.types.flow.control.ForNode
import me.anno.graph.types.flow.control.IfElseNode
import me.anno.graph.types.flow.control.WhileNode
import me.anno.graph.types.flow.local.GetLocalVariableNode
import me.anno.graph.types.flow.local.SetLocalVariableNode
import me.anno.graph.types.flow.maths.*
import me.anno.graph.types.flow.vector.*
import me.anno.graph.types.states.StateMachine
import me.anno.io.ISaveable.Companion.registerCustomClass

class NodeLibrary(val nodes: Collection<() -> Node>) {

    val allNodes: List<Pair<Node, () -> Node>> = nodes.map { gen ->
        val sample = gen()
        if (sample is EnumNode) sample.listNodes().map {
            it to { it.clone() }
        }
        else listOf(sample to gen)
    }.flatten()

    constructor(vararg nodes: () -> Node) :
            this(nodes.toList())

    fun register() {
        for (node in nodes) {
            registerCustomClass(node)
        }
    }

    companion object {

        val flowNodes = NodeLibrary(
            { GetLocalVariableNode() },
            { GetLocalVariableNode("Boolean") },
            { GetLocalVariableNode("Float") },
            { GetLocalVariableNode("Int") },
            { GetLocalVariableNode("Vector2f") },
            { GetLocalVariableNode("Vector3f") },
            { GetLocalVariableNode("Vector4f") },
            { SetLocalVariableNode() },
            { SetLocalVariableNode("Boolean") },
            { SetLocalVariableNode("Float") },
            { SetLocalVariableNode("Int") },
            { SetLocalVariableNode("Vector2f") },
            { SetLocalVariableNode("Vector3f") },
            { SetLocalVariableNode("Vector4f") },
            { ValueNode("Float") },
            { ValueNode("Double") },
            { ValueNode("Int") },
            { ValueNode("Long") },
            { ValueNode("Boolean") },
            { ValueNode("String") },
            { ConfigGetBoolNode() },
            { ConfigGetIntNode() },
            { ConfigGetFloatNode() },
            { ForNode() },
            { IfElseNode() },
            { WhileNode() },
            { DoWhileNode() },
            { PrintNode() },
            { NotNode() },
            { MathB2Node() },
            { MathB3Node() },
            { MathL1Node() },
            { MathL2Node() },
            { MathL3Node() },
            { MathI1Node() },
            { MathI2Node() },
            { MathI3Node() },
            { MathD1Node() },
            { MathD2Node() },
            { MathD3Node() },
            { MathF1Node() },
            { MathF2Node() },
            { MathF3Node() },
            { MathF12Node() },
            { MathF22Node() },
            { MathF32Node() },
            { MathF13Node() },
            { MathF23Node() },
            { MathF33Node() },
            { MathF14Node() },
            { MathF24Node() },
            { MathF34Node() },
            { DotProductF2() },
            { DotProductF3() },
            { DotProductF4() },
            { DotProductD2() },
            { DotProductD3() },
            { DotProductD4() },
            { CrossProductF2() },
            { CrossProductF3() },
            { CrossProductD2() },
            { CrossProductD3() },
            { RotateF2Node() },
            { RotateF3XNode() },
            { RotateF3YNode() },
            { RotateF3ZNode() },
            { NormalizeNode2() },
            { NormalizeNode3() },
            { NormalizeNode4() },
            { FresnelNode3() },
            { CompareNode() },
            { CompareNode("Double") },
            { CompareNode("Long") },
            { CombineVector2f() },
            { CombineVector3f() },
            { CombineVector4f() },
            { SeparateVector2f() },
            { SeparateVector3f() },
            { SeparateVector4f() },
            { ColorNode() },
            { GameTime() },
            { RandomNode() },
        )

        fun registerClasses() {
            registerCustomClass(NodeInput())
            registerCustomClass(NodeOutput())
            registerCustomClass(AnimStateNode())
            // cloning this is complicated, so let's use a real constructor
            registerCustomClass { StateMachine() }
            flowNodes.register()
        }
    }
}