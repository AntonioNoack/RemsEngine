package me.anno.games.visualnovel

import me.anno.games.visualnovel.VisualNovelState.setText
import me.anno.graph.visual.states.StateNode
import me.anno.ui.UIColors
import me.anno.utils.Color

class SpeechNode() : StateNode("Speak", listOf("String", "Text"), emptyList()) {
    constructor(text: String) : this() {
        setInput(1, text)
    }

    override fun onEnterState(oldState: StateNode?) {
        color = UIColors.gold
        setText(getInput(1).toString(), 0, this)
    }

    override fun onExitState(newState: StateNode?) {
        color = Color.black
    }
}