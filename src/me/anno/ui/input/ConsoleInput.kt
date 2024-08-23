package me.anno.ui.input

import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.maths.Maths.clamp

@Suppress("unused")
class ConsoleInput(nameDesc: NameDesc, enableSuggestions: Boolean, style: Style) :
    TextInput(nameDesc, "", enableSuggestions, style) {

    @Suppress("unused")
    constructor(style: Style): this(NameDesc.EMPTY, true, style)

    var actionListener: (String) -> Unit = {}

    var indexFromTop = 0

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "MoveUp" -> moveUp()
            "MoveDown" -> moveDown()
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun setActionListener(listener: (String) -> Unit): ConsoleInput {
        this.actionListener = listener
        return this
    }

    override fun onEnterKey(x: Float, y: Float) {
        history.remove(value)
        history.add(value)
        resetIndex()
        actionListener(value)
    }

    fun resetIndex() {
        indexFromTop = -1
    }

    fun moveDown() {
        indexFromTop = clamp(indexFromTop-1, -1, history.lastIndex)
        showHistoryEntry()
    }

    fun moveUp() {
        indexFromTop = clamp(indexFromTop-1, -1, history.lastIndex)
        showHistoryEntry()
    }

    fun showHistoryEntry() {
        if (indexFromTop in 0 until history.size) {
            setValue(history[history.lastIndex - indexFromTop], false)
        }
    }

    companion object {
        @JvmField
        val history = ArrayList<String>()
        // todo save the history, maybe per instance...
        // todo also load it ;)
    }

}