package me.anno.games.visualnovel

import me.anno.games.visualnovel.VisualNovelState.setText
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.states.StateNode
import me.anno.ui.UIColors
import me.anno.utils.Color
import me.anno.utils.types.Strings.isNotBlank2

class QuestionNode() : StateNode("Question", listOf("String", "Question"), listOf("Int", "Result")) {
    constructor(question: String, options: List<String>) : this() {
        setInput(1, question)
        inputs.addAll(options.map { NodeInput("String", this, true) })
        for (i in options.indices) {
            setInput(i + 2, options[i])
        }
    }

    override fun onEnterState(oldState: StateNode?) {
        color = UIColors.gold
        val options = (2 until inputs.size)
            .map { (getInput(it) ?: "").toString() }
            .filter { it.isNotBlank2() }
        val shownText = "${getInput(1) ?: ""}\n" +
                options.withIndex().joinToString("\n") { (idx, option) ->
                    "  [${idx + 1}] $option"
                }
        setText(shownText, options.size, this)
    }

    override fun onExitState(newState: StateNode?) {
        color = Color.black
    }

    override fun canAddInput(type: String, index: Int) = index > 0 && type == "String"
}
