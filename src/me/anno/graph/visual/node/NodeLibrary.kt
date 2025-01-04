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
        val flowNodes by lazy {
            // ensure all available nodes have been registered
            ECSRegistry.init()

            val typeNames = "?,String,Boolean,Float,Int,Vector2f,Vector3f,Vector4f"
                .split(',')
            val nodeClassNames = Saveable.objectTypeRegistry.keys
                .filter { it.endsWith("Node") }

            NodeLibrary(
                typeNames.map { typeName ->
                    { GetLocalVariableNode(typeName) }
                } + typeNames.map { typeName ->
                    { SetLocalVariableNode(typeName) }
                } + typeNames.map { typeName ->
                    { ValueNode(typeName) }
                } + typeNames.map { typeName ->
                    { InlineBranchNode(typeName) }
                } + nodeClassNames
                    .filter { name -> Saveable.createOrNull(name) is Node }
                    .map { name -> { Saveable.createOrNull(name) as Node } }
            )
        }
    }
}