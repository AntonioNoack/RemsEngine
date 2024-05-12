package me.anno.graph.visual

import me.anno.graph.visual.node.Node

/**
 * Node with internal state, which modifies connector types or behaviour;
 * UI shall know of all nodes though, so this interface provides a way to get all permutations.
 *
 * The list size shall be reasonable, and each node shall have a unique name.
 * */
interface EnumNode {
    fun listNodes(): List<Node>
}