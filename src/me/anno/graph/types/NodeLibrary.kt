package me.anno.graph.types

import me.anno.engine.ECSRegistry
import me.anno.graph.EnumNode
import me.anno.graph.Node
import me.anno.graph.types.flow.local.GetLocalVariableNode
import me.anno.graph.types.flow.local.SetLocalVariableNode
import me.anno.graph.types.flow.maths.CompareNode
import me.anno.graph.types.flow.maths.ValueNode
import me.anno.io.Saveable

class NodeLibrary(val nodes: Collection<() -> Node>) {

    val allNodes: List<Pair<Node, () -> Node>> = nodes.flatMap { gen ->
        val sample = gen()
        if (sample is EnumNode) sample.listNodes().map {
            it to { it.clone() }
        }
        else listOf(sample to gen)
    }

    companion object {

        private const val NODE_CLASS_NAMES = "" +
                "ConfigGetBoolNode,ConfigGetIntNode,ConfigGetFloatNode,ForNode,IfElseNode,WhileNode,DoWhileNode," +
                "PrintNode,NotNode,MathB2Node,MathB3Node,MathL1Node,MathL2Node,MathL3Node,MathI1Node,MathI2Node," +
                "MathI3Node,MathD1Node,MathD2Node,MathD3Node,MathF1Node,MathF2Node,MathF3Node,MathF12Node,MathF22Node," +
                "MathF32Node,MathF13Node,MathF23Node,MathF33Node,MathF14Node,MathF24Node,MathF34Node,DotProductF2," +
                "DotProductF3,DotProductF4,DotProductD2,DotProductD3,DotProductD4,CrossProductF2,CrossProductF3," +
                "CrossProductD2,CrossProductD3,RotateF2Node,RotateF3XNode,RotateF3YNode,RotateF3ZNode,NormalizeNode2," +
                "NormalizeNode3,NormalizeNode4,FresnelNode3,CompareNode,CombineVector2f,CombineVector3f,CombineVector4f," +
                "SeparateVector2f,SeparateVector3f,SeparateVector4f,ColorNode,GameTime,RandomNode"

        private val typeNames = "?,String,Boolean,Float,Int,Vector2f,Vector3f,Vector4f".split(',')

        init {
            // ensure all available nodes have been registered
            ECSRegistry.init()
        }

        val flowNodes = NodeLibrary(
            listOf(
                { CompareNode("Double") },
                { CompareNode("Long") },
            ) + typeNames.map { typeName ->
                { GetLocalVariableNode(typeName) }
            } + typeNames.map { typeName ->
                { SetLocalVariableNode(typeName) }
            } + typeNames.map { typeName ->
                { ValueNode(typeName) }
            } + NODE_CLASS_NAMES.split(',').map { name ->
                { Saveable.create(name) as Node }
            }
        )
    }
}