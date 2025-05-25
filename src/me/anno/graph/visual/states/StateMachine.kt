package me.anno.graph.visual.states

import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.FlowGraphNode
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager

/**
 * given an initial state, compute the next state & update internal values
 * */
class StateMachine : FlowGraph() {

    companion object {
        private val LOGGER = LogManager.getLogger(StateMachine::class)
    }

    var prevState: StateNode? = null
    var state: StateNode? = null

    fun start(startNode: FlowGraphNode): StateNode? {
        return next(execute(startNode) as? StateNode)
    }

    private fun findState(): StateNode? {
        var state = state
        if (state == null) {
            state = findDefaultState() ?: return null
            if (!"default".equals(state.name, true)) {
                LOGGER.warn("Missing node with name 'Default' for default state")
            }
            state.onEnterState(null)
        }
        return state
    }

    fun update(): StateNode? {
        val prevState = findState() ?: return null
        return next(prevState.update())
    }

    private fun findDefaultState(): StateNode? {
        return nodes.firstOrNull2 { it is StateNode && "default".equals(it.name, true) } as? StateNode
            ?: nodes.firstOrNull2 { it is StateNode } as? StateNode
    }

    fun next(newState: StateNode?): StateNode? {
        invalidate()
        val oldState = state
        if (oldState !== newState) {
            oldState?.onExitState(newState)
            newState?.onEnterState(oldState)
            state = newState
            prevState = oldState
        }
        return newState
    }
}