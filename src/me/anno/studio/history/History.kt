package me.anno.studio.history

import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.studio.history.HistoryState.Companion.capture
import org.apache.logging.log4j.LogManager
import kotlin.math.max

class History : Saveable() {

    var currentState: HistoryState? = null
    var nextInsertIndex = 0
        set(value) {
            field = max(value, 0)
        }

    private val states = ArrayList<HistoryState>()

    fun isEmpty() = states.isEmpty()

    fun clearToSize() {
        while (states.size > maxChanged && maxChanged > 0) {
            states.removeAt(0)
        }
    }

    fun update(title: String, code: Any) {
        val last = states.lastOrNull()
        if (last?.code == code) {
            last.capture(last)
            last.title = title
        } else {
            put(title, code)
        }
    }

    fun put(change: HistoryState): Int {
        // remove states at the top of the stack...
        // while (states.size > nextInsertIndex) states.removeAt(states.lastIndex)
        states += change
        clearToSize()
        nextInsertIndex = states.size
        return nextInsertIndex
    }

    fun put(title: String, code: Any) {
        val nextState = capture(title, code, currentState)
        if (nextState != currentState) {
            put(nextState)
            currentState = nextState
        }
    }

    fun redo() {
        if (nextInsertIndex < states.size) {
            states[nextInsertIndex].apply()
            nextInsertIndex++
        } else LOGGER.info("Nothing left to redo!")
    }

    fun undo() {
        if (nextInsertIndex > 1) {
            nextInsertIndex--
            states[nextInsertIndex - 1].apply()
        } else LOGGER.info("Nothing left to undo!")
    }

    private fun redo(index: Int) {
        states.getOrNull(index)?.apply {
            // put(this)
            apply()
        }
    }

    fun display() {
        GFX.openMenu("Change History", states.mapIndexed { index, change ->
            val title = if (index == nextInsertIndex - 1) "* ${change.title}" else change.title
            GFX.MenuOption(title, "Click to redo") {
                redo(index)
            }
        }.reversed())
    }

    // todo file explorer states?

    override fun readInt(name: String, value: Int) {
        when (name) {
            "nextInsertIndex" -> nextInsertIndex = value
            else -> super.readInt(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "state" -> {
                states += value as? HistoryState ?: return
            }
            else -> super.readObject(name, value)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("nextInsertIndex", nextInsertIndex)
        states.forEach { state ->
            writer.writeObject(this, "state", state)
        }
    }

    override fun getApproxSize(): Int = 1_500_000_000
    override fun isDefaultValue(): Boolean = false
    override fun getClassName(): String = "History"

    companion object {
        val LOGGER = LogManager.getLogger(History::class)
        val maxChanged = 512
    }

}