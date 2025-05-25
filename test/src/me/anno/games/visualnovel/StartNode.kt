package me.anno.games.visualnovel

import me.anno.games.visualnovel.VisualNovelState.graphEditor
import me.anno.games.visualnovel.VisualNovelState.setText
import me.anno.graph.visual.states.StateMachine
import me.anno.graph.visual.states.StateNode
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList
import me.anno.ui.editor.graph.GraphPanel

class StartNode : StateNode("Start") {
    override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
        val self = this
        val graph = graph as StateMachine
        super.createUI(g, list, style)
        list.add(
            TextButton(NameDesc("Start"), style)
                .addLeftClickListener {
                    graph.start(self) // sets self
                    graph.update() // finds first true node
                })
        setText("", 0, this)
    }

    override fun onEnterState(oldState: StateNode?) {
        graphEditor?.requestFocus(this)
    }
}