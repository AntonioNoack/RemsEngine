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
import me.anno.graph.types.flow.maths.CompareNode
import me.anno.graph.types.flow.maths.CrossProductD2
import me.anno.graph.types.flow.maths.CrossProductD3
import me.anno.graph.types.flow.maths.CrossProductF2
import me.anno.graph.types.flow.maths.CrossProductF3
import me.anno.graph.types.flow.maths.DotProductD2
import me.anno.graph.types.flow.maths.DotProductD3
import me.anno.graph.types.flow.maths.DotProductD4
import me.anno.graph.types.flow.maths.DotProductF2
import me.anno.graph.types.flow.maths.DotProductF3
import me.anno.graph.types.flow.maths.DotProductF4
import me.anno.graph.types.flow.maths.MathB2Node
import me.anno.graph.types.flow.maths.MathB3Node
import me.anno.graph.types.flow.maths.MathD1Node
import me.anno.graph.types.flow.maths.MathD2Node
import me.anno.graph.types.flow.maths.MathD3Node
import me.anno.graph.types.flow.maths.MathF1Node
import me.anno.graph.types.flow.maths.MathF2Node
import me.anno.graph.types.flow.maths.MathF3Node
import me.anno.graph.types.flow.maths.MathI1Node
import me.anno.graph.types.flow.maths.MathI2Node
import me.anno.graph.types.flow.maths.MathI3Node
import me.anno.graph.types.flow.maths.MathL1Node
import me.anno.graph.types.flow.maths.MathL2Node
import me.anno.graph.types.flow.maths.MathL3Node
import me.anno.graph.types.flow.maths.NotNode
import me.anno.graph.types.flow.maths.ValueNode
import me.anno.graph.types.flow.vector.CombineVector2f
import me.anno.graph.types.flow.vector.CombineVector3f
import me.anno.graph.types.flow.vector.CombineVector4f
import me.anno.graph.types.flow.vector.FresnelNode3
import me.anno.graph.types.flow.vector.MathF12Node
import me.anno.graph.types.flow.vector.MathF13Node
import me.anno.graph.types.flow.vector.MathF14Node
import me.anno.graph.types.flow.vector.MathF22Node
import me.anno.graph.types.flow.vector.MathF23Node
import me.anno.graph.types.flow.vector.MathF24Node
import me.anno.graph.types.flow.vector.MathF32Node
import me.anno.graph.types.flow.vector.MathF33Node
import me.anno.graph.types.flow.vector.MathF34Node
import me.anno.graph.types.flow.vector.NormalizeNode2
import me.anno.graph.types.flow.vector.NormalizeNode3
import me.anno.graph.types.flow.vector.NormalizeNode4
import me.anno.graph.types.flow.vector.RotateF2Node
import me.anno.graph.types.flow.vector.RotateF3XNode
import me.anno.graph.types.flow.vector.RotateF3YNode
import me.anno.graph.types.flow.vector.RotateF3ZNode
import me.anno.graph.types.flow.vector.SeparateVector2f
import me.anno.graph.types.flow.vector.SeparateVector3f
import me.anno.graph.types.flow.vector.SeparateVector4f
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