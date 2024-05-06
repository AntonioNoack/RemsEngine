package me.anno.graph.render.effects

import me.anno.graph.types.flow.FlowGraphNodeUtils.getIntInput
import me.anno.graph.types.flow.actions.ActionNode

/**
 * This helper sets "Samples", so QuickPipeline connects then automatically.
 * */
class MSAAHelperNode : ActionNode(
    "MSAAHelper",
    listOf("Int", "Samples"),
    listOf("Int", "Samples")
) {

    init {
        setInput(1, 8)
    }

    override fun executeAction() {
        setOutput(1, getIntInput(1))
    }
}