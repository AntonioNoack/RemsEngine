package me.anno.graph.visual.node

import me.anno.engine.ECSRegistry
import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.control.InlineBranchNode
import me.anno.graph.visual.local.GetLocalVariableNode
import me.anno.graph.visual.local.SetLocalVariableNode
import me.anno.graph.visual.scalar.ValueNode
import me.anno.io.Saveable

class NodeLibrary(val nodes: Collection<() -> Node>) {

    val allNodes: List<Pair<Node, () -> Node>> = nodes.flatMap { gen ->
        val sample = gen()
        if (sample is EnumNode) {
            sample.listNodes().map {
                it to { it.clone() }
            }
        } else {
            listOf(sample to gen)
        }
    }

    companion object {

        private const val NODE_CLASS_NAMES = "" +
                "ConfigGetBoolNode,ConfigGetIntNode,ConfigGetFloatNode,ForNode,IfElseNode,WhileNode,DoWhileNode," +
                "PrintNode,NotNode,MathB2Node,MathB3Node,MathL1Node,MathL2Node,MathL3Node,MathI1Node,MathI2Node," +
                "MathI3Node,MathD1Node,MathD2Node,MathD3Node,MathF1Node,MathF2Node,MathF3Node,MathF1XNode,MathF2XNode," +
                "MathF3XNode,DotProductF2,DotProductF3,DotProductF4," +
                "DotProductD2,DotProductD3,DotProductD4," +
                "CrossProductF2,CrossProductF3,CrossProductD2,CrossProductD3," +
                "RotateF2Node,RotateF3XNode,RotateF3YNode,RotateF3ZNode," +
                "NormalizeNode2,NormalizeNode3,NormalizeNode4,FresnelNode3," +
                "CompareNode,CombineVector2f,CombineVector3f,CombineVector4f," +
                "SeparateVector2f,SeparateVector3f,SeparateVector4f,ColorNode,GameTime,RandomNode," +
                "VectorLengthNode,VectorDistanceNode"

        private const val TYPE_NAMES = "?,String,Boolean,Float,Int,Vector2f,Vector3f,Vector4f"

        val flowNodes by lazy {
            // ensure all available nodes have been registered
            val typeNames = TYPE_NAMES.split(',')
            ECSRegistry.init()
            NodeLibrary(
                typeNames.map { typeName ->
                    { GetLocalVariableNode(typeName) }
                } + typeNames.map { typeName ->
                    { SetLocalVariableNode(typeName) }
                } + typeNames.map { typeName ->
                    { ValueNode(typeName) }
                } + typeNames.map { typeName ->
                    { InlineBranchNode(typeName) }
                } + NODE_CLASS_NAMES.split(',').map { name ->
                    { Saveable.create(name) as Node }
                }
            )
        }
    }
}