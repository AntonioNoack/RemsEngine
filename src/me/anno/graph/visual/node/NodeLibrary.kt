package me.anno.graph.visual.node

import me.anno.engine.ECSRegistry
import me.anno.graph.visual.EnumNode
import me.anno.graph.visual.control.InlineBranchNode
import me.anno.graph.visual.local.GetLocalVariableNode
import me.anno.graph.visual.local.SetLocalVariableNode
import me.anno.graph.visual.scalar.ValueNode
import me.anno.io.saveable.Saveable

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
                "ConfigGetBoolNode,ConfigGetIntNode,ConfigGetFloatNode,ForNode,IfElseNode,WhileNode,DoWhileNode,PrintNode," +
                "NotNode,MathB2Node,MathB3Node," +
                "MathI1Node,MathI2Node,MathI3Node," +
                "MathF1Node,MathF2Node,MathF3Node," +
                "MathF1XNode,MathF2XNode,MathF3XNode," +
                "DotProductNode,CrossProductNode,NormalizeNode," +
                "RotateF2Node,RotateF3XNode,RotateF3YNode,RotateF3ZNode," +
                "FresnelNode3,CompareNode,CombineVectorNode,SeparateVectorNode," +
                "ColorNode,GameTime,RandomNode," +
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
                } + NODE_CLASS_NAMES.split(',')
                    .filter { name -> Saveable.createOrNull(name) is Node }
                    .map { name -> { Saveable.createOrNull(name) as Node } }
            )
        }
    }
}