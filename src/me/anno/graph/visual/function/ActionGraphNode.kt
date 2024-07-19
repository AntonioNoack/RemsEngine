package me.anno.graph.visual.function

import me.anno.graph.visual.actions.ActionNode

class ActionGraphNode(name: String, inputs: List<String>, outputs: List<String>) :
    ActionNode(name, inputs, outputs) {
    override fun executeAction() {
        TODO("Not yet implemented")
    }
}