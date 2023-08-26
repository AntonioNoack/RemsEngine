package me.anno.graph.types.states

import me.anno.graph.Node
import me.anno.graph.types.ControlFlowGraph
import me.anno.utils.structures.lists.Lists.firstOrNull2
import org.apache.logging.log4j.LogManager

/**
 * given an initial state, compute the next state & update internal values
 * */
class StateMachine : ControlFlowGraph() {

    companion object {
        private val LOGGER = LogManager.getLogger(StateMachine::class)
    }

    var prevState: StateNode? = null
    var state: StateNode? = null

    fun start(startNode: Node): StateNode? {
        val newState = try {
            execute(startNode)
            null
        } catch (e: NewState) {
            e.state
        }
        return next(newState)
    }

    fun update(): StateNode? {
        var oldState = state
        if (oldState == null) {
            // find default state
            oldState = nodes.firstOrNull2 { it is StateNode && "default".equals(it.name, true) } as? StateNode
                ?: nodes.firstOrNull2 { it is StateNode } as? StateNode
            if (oldState == null) return null
            if (!"default".equals(oldState.name, true)) {
                LOGGER.warn("Missing node with name 'Default' for default state")
            }
            oldState.onEnterState(null)
        }
        return next(oldState.update())
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

    override val className: String get() = "StateMachine"
}