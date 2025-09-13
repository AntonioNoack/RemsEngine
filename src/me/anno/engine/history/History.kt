package me.anno.engine.history

import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import org.apache.logging.log4j.LogManager
import java.util.LinkedList
import kotlin.math.max

/**
 * Use this class in the following way:
 * Firstly, once you create a History instance, you should push the initial state into it.
 * Then on every change, first apply your change, then push that changed state into the History instance.
 *
 * Override the 'apply()'-function to deserialize/use the given curr-state, and apply it to your scene, e.g., render that one going forward.
 * For key presses, handle Redo and Undo, and lead them into this instance.
 * If you're using Entities/Prefabs, Control+Z/Y may be captured by global actions before you could get them.
 * You can override them using ActionManager.registerGlobalAction.
 *
 * The maximum number of stored states is by default 512. You can change Companion.maxChanged to adjust that.
 * */
abstract class History<V : Any>(startState: V) : Saveable() {

    /**
     * Deserialize/use the given curr-state, and apply it to your scene, e.g., render that one going forward.
     * */
    abstract fun apply(prev: V, curr: V)

    /**
     * What to show for each item in the History-Panel (Control+H).
     * */
    abstract fun getTitle(v: V): String

    /**
     * Check whether v is a valid state instance.
     * (Important after deserialization)
     * */
    abstract fun filter(v: Any?): V?

    var currentState: V = startState
    var nextInsertIndex: Int = 0
        set(value) {
            field = max(value, 0)
        }

    val states = LinkedList<V>()

    fun isEmpty() = states.isEmpty()

    val numStates get() = states.size

    /**
     * Removes the first states until targetSize has been reached.
     * Most times, this will do nothing or just pop one element.
     * */
    fun clearToSize(targetSize: Int = maxChanged) {
        synchronized(states) {
            val toRemove = states.size - targetSize
            if (toRemove > 0) {
                states.subList(0, toRemove).clear()
                nextInsertIndex -= toRemove
            }
        }
    }

    /**
     * Push the initial state or any new state.
     * This function returns the index if you want to jump back to this state using redo(index).
     * */
    fun put(change: V): Int {
        return synchronized(states) {
            states.add(change)
            clearToSize()
            nextState(states.size)
            nextInsertIndex
        }
    }

    /**
     * Mark the next state as #nextInsertIndex.
     * Apply any changes and set the current state.
     * */
    fun nextState(nextInsertIndex: Int) {
        this.nextInsertIndex = nextInsertIndex
        nextState()
    }

    /**
     * Apply any changes and set the current state.
     * */
    fun nextState() {
        val oldState = currentState
        val newState = states[nextInsertIndex - 1]
        apply(oldState, newState)
        currentState = newState
    }

    /**
     * Go forward one step. Only possible after first undoing something.
     * Returns whether that was possible.
     * */
    fun redo(): Boolean {
        return synchronized(states) {
            if (nextInsertIndex < states.size) {
                nextState(nextInsertIndex + 1)
                true
            } else {
                LOGGER.info("Nothing left to redo!")
                false
            }
        }
    }

    /**
     * Go back one step. Returns whether that was possible.
     * */
    fun undo(delta: Int = 1): Boolean {
        if (delta <= 0) return false
        return synchronized(states) {
            val newNextInsertIndex = nextInsertIndex - delta
            if (newNextInsertIndex >= 1) {
                nextState(newNextInsertIndex)
                true
            } else {
                LOGGER.info("Nothing left to undo!")
                false
            }
        }
    }

    fun redo(index: Int) {
        if (index !in states.indices) return
        nextState(index + 1)
    }

    fun display() {
        openMenu(
            GFX.someWindow.windowStack,
            NameDesc("Inspect History", "", "ui.inspectHistory"),
            List(states.size) {
                val index = states.lastIndex - it
                val change = states[index]
                val title0 = getTitle(change)
                val title = if (index == nextInsertIndex - 1) "* $title0" else title0
                MenuOption(NameDesc(title, Dict["Click to redo", "ui.history.clickToUndo"], "")) {
                    redo(index)
                }
            }
        )
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "nextInsertIndex" -> nextInsertIndex = value as? Int ?: return
            "state" -> states.add(filter(value) ?: return)
            "states" -> {
                states.clear()
                states.addAll((value as List<*>).mapNotNull { filter(it) })
            }
            else -> super.setProperty(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("nextInsertIndex", nextInsertIndex)
        if (states.isNotEmpty()) {
            synchronized(states) {
                saveStates(writer)
            }
        }
    }

    open fun saveStates(writer: BaseWriter) {
        writer.writeSomething(null, "states", states, true)
    }

    override val approxSize get() = 1_500_000_000

    companion object {
        private val LOGGER = LogManager.getLogger(History::class)
        var maxChanged = 512
    }
}