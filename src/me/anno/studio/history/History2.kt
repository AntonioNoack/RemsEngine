package me.anno.studio.history

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.studio.StudioBase.Companion.defaultWindowStack
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import org.apache.logging.log4j.LogManager
import kotlin.math.max

abstract class History2<V> : Saveable() {

    abstract fun apply(v: V)
    abstract fun getTitle(v: V): String
    abstract fun filter(v: Any?): V?

    var currentState: V? = null
    var nextInsertIndex = 0
        set(value) {
            field = max(value, 0)
        }

    private val states = ArrayList<V>()

    fun isEmpty() = states.isEmpty()

    fun clearToSize() {
        synchronized(states) {
            while (states.size > maxChanged && maxChanged > 0) {
                states.removeAt(0)
            }
        }
    }

    fun put(change: V): Int {
        synchronized(states) {
            // remove states at the top of the stack...
            // while (states.size > nextInsertIndex) states.removeAt(states.lastIndex)
            states.add(change)
            clearToSize()
            nextInsertIndex = states.size
            return nextInsertIndex
        }
    }

    fun redo() {
        synchronized(states) {
            if (nextInsertIndex < states.size) {
                apply(states[nextInsertIndex])
                nextInsertIndex++
            } else LOGGER.info("Nothing left to redo!")
        }
    }

    fun undo() {
        synchronized(states) {
            if (nextInsertIndex > 1) {
                nextInsertIndex--
                apply(states[nextInsertIndex - 1])
            } else LOGGER.info("Nothing left to undo!")
        }
    }

    private fun redo(index: Int) {
        apply(states.getOrNull(index) ?: return)
    }

    fun display() {
        openMenu(defaultWindowStack!!, NameDesc("Inspect History", "", "ui.inspectHistory"), states.mapIndexed { index, change ->
            val title0 = getTitle(change)
            val title = if (index == nextInsertIndex - 1) "* $title0" else title0
            MenuOption(NameDesc(title, Dict["Click to redo", "ui.history.clickToUndo"], "")) {
                redo(index)
            }
        }.reversed())
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "nextInsertIndex" -> nextInsertIndex = value
            else -> super.readInt(name, value)
        }
    }

    override fun readSomething(name: String, value: Any?) {
        when (name) {
            "state" -> states.add(filter(value) ?: return)
            "states" -> {
                states.clear()
                states.addAll((value as Array<*>).mapNotNull { filter(it) })
            }
            else -> super.readSomething(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("nextInsertIndex", nextInsertIndex)
        synchronized(states) {
            /*for (state in states) {
                writer.writeSomething(null, "state", state, true)
            }*/
            writer.writeSomething(null, "states", states, true)
        }
    }

    override val approxSize get() = 1_500_000_000
    override fun isDefaultValue(): Boolean = false

    companion object {
        val LOGGER = LogManager.getLogger(History2::class)
        val maxChanged = 512
    }

}