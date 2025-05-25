package me.anno.games.visualnovel

import me.anno.config.DefaultConfig
import me.anno.engine.Events.addEvent
import me.anno.engine.OfficialExtensions
import me.anno.engine.WindowRenderFlags.showFPS
import me.anno.games.visualnovel.VisualNovelState.graphEditor
import me.anno.graph.visual.control.IfElseNode
import me.anno.graph.visual.node.NodeLibrary
import me.anno.graph.visual.scalar.CompareNode
import me.anno.graph.visual.states.StateMachine
import me.anno.ui.Panel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.graph.GraphEditor

fun createSampleDialog(stateMachine: StateMachine) {

    // add a sample dialog :3
    val start = StartNode()
    val intro = SpeechNode("Hi, Darling!")
    val question = QuestionNode(
        "Do you still love me?", listOf(
            "Of course, I do",
            "More so than ever",
            "No", "Who are you?"
        )
    )
    val decision = IfElseNode()
    val comp = CompareNode("Int") // <
    val positiveEnding = SpeechNode("Daisuki 😍")
    val negativeEnding = SpeechNode("Oh 😥")

    start.connectTo(intro)
    intro.connectTo(question)
    question.connectTo(decision)
    decision.connectTo(positiveEnding)
    decision.connectTo(1, negativeEnding, 0)
    question.connectTo(1, comp, 0)
    comp.setInput(1, 3)
    comp.connectTo(decision, 1)

    start.position.set(-160.0, -160.0, 0.0)
    intro.position.set(+160.0, -160.0, 0.0)
    question.position.set(-100.0, 0.0, 0.0)
    comp.position.set(230.0, 0.0, 0.0)
    decision.position.set(0.0, 230.0, 0.0)
    positiveEnding.position.set(230.0, 160.0, 0.0)
    negativeEnding.position.set(230.0, 300.0, 0.0)

    // restart
    val restart = SpeechNode("Restarting...")
    positiveEnding.connectTo(restart)
    negativeEnding.connectTo(restart)
    restart.connectTo(start)
    restart.position.set(450.0, 230.0, 0.0)

    stateMachine.addAllConnected(start)
}

val visualNovelNodeLibrary = NodeLibrary(
    NodeLibrary.flowNodes.nodes + listOf(
        { SpeechNode() },
        { QuestionNode() },
        { CharacterNode() },
        { SceneNode() }
    ))

fun createEditorPanel(stateMachine: StateMachine): Panel {
    val graphPanel = GraphEditor(stateMachine, DefaultConfig.style)
    graphPanel.library = visualNovelNodeLibrary
    graphEditor = graphPanel
    return graphPanel
}

/**
 * This sample shows how a simple visual novel could be programmed, and dialogs be created using the GraphEditor.
 * The visual novel is built using a StateMachine, which is just a special graph for such a purpose.
 * */
fun main() {

    OfficialExtensions.initForTests()

    // todo load good images from somewhere...

    // to do screen shake for background??
    // to do soft moving of background (plus scale maybe)?

    val stateMachine = StateMachine()
    createSampleDialog(stateMachine)

    val splitter = CustomList(false, DefaultConfig.style)
    splitter.add(createEditorPanel(stateMachine))
    splitter.add(NovelGamePanel(stateMachine))

    testUI("Visual Novel", splitter)
}