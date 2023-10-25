package me.anno.tests.game

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.graph.Node
import me.anno.graph.NodeInput
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.NodeLibrary.Companion.flowNodes
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.graph.types.flow.control.IfElseNode
import me.anno.graph.types.flow.maths.CompareNode
import me.anno.graph.types.states.StateMachine
import me.anno.graph.types.states.StateNode
import me.anno.graph.ui.GraphEditor
import me.anno.graph.ui.GraphPanel
import me.anno.graph.ui.GraphPanel.Companion.yellow
import me.anno.image.ImageGPUCache
import me.anno.image.ImageScale
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.TextPanel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.Style
import me.anno.utils.Color.black
import me.anno.utils.Color.mulARGB
import me.anno.utils.OS.downloads
import me.anno.utils.OS.pictures
import kotlin.math.max

/**
 * This sample shows how a simple visual novel could be programmed, and dialogs be created using the GraphEditor.
 * The visual novel is built using a StateMachine, which is just a special graph for such a purpose.
 * */
object VisualNovel {

    var graphEditor: GraphEditor? = null

    val samples = downloads.getChild("sprites")
    var background: FileReference = pictures.getChild("4k.jpg")
    var primary: FileReference = samples.getChild("anise_c_laugh.png")
    var secondary: FileReference = samples.getChild("cardamom_nc_normal.png")

    var shownText = ""
    var textTime = 0L
    var numOptions: Int = 0
    var questionNode: QuestionNode? = null

    fun setText(text: String, options: Int, node: Node) {
        shownText = text
        numOptions = options
        questionNode = node as? QuestionNode
        textTime = 0
    }

    class SpeechNode() : StateNode("Speak", listOf("String", "Text"), emptyList()) {
        constructor(text: String) : this() {
            setInput(1, text)
        }

        override fun onEnterState(oldState: StateNode?) {
            color = yellow
            setText(getInput(1).toString(), 0, this)
        }

        override fun onExitState(newState: StateNode?) {
            color = black
        }
    }

    class CharacterNode : ActionNode(
        "Characters",
        listOf("FileReference", "Primary", "FileReference", "Secondary"), emptyList()
    ) {
        override fun executeAction() {
            primary = getInput(1) as? FileReference ?: InvalidRef
            secondary = getInput(2) as? FileReference ?: InvalidRef
        }
    }

    class QuestionNode() : StateNode("Question", listOf("String", "Question"), listOf("Int", "Result")) {
        constructor(question: String, options: List<String>) : this() {
            setInput(1, question)
            inputs = inputs!! + options.map { NodeInput("String", this, true) }
            for (i in options.indices) {
                setInput(i + 2, options[i])
            }
        }

        override fun onEnterState(oldState: StateNode?) {
            color = yellow
            val options = (2 until inputs!!.size).map { (getInput(it) ?: "").toString() }.filter { it.isNotBlank() }
            val shownText = (getInput(1) ?: "").toString() + "\n" +
                    options.withIndex().joinToString("") { (idx, it) -> "  [${idx + 1}] $it\n" }
            setText(shownText, options.size, this)
        }

        override fun onExitState(newState: StateNode?) {
            color = black
        }

        override fun canAddInput(type: String, index: Int) = index > 0 && type == "String"
    }

    class SceneNode : ActionNode("Scene", listOf("FileReference", "Background"), emptyList()) {
        override fun executeAction() {
            background = getInput(1) as? FileReference ?: InvalidRef
        }
    }

    class StartNode : StateNode("Start") {
        override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
            val self = this
            val graph = graph as StateMachine
            super.createUI(g, list, style)
            list.add(TextButton("Start", style)
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

    @JvmStatic
    fun main(args: Array<String>) {

        // story graph
        // nodes for changing character images, background, effects and such,
        // todo load good images from somewhere...

        // UI for this
        //  - background image
        //  - characters talking

        // UI split in two: graph, and game
        // start = restart button

        val stateMachine = StateMachine()
        val library = NodeLibrary(flowNodes.nodes + listOf(
            { SpeechNode() },
            { QuestionNode() },
            { CharacterNode() },
            { SceneNode() }
        ))

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

        val splitter = CustomList(false, style)
        val graphPanel = GraphEditor(stateMachine, style)
        graphPanel.library = library
        graphEditor = graphPanel

        // to do screen shake for background??
        // to do soft moving of background (plus scale maybe)?

        val gamePanel = object : PanelList(style) {
            override val canDrawOverBorders: Boolean get() = true

            val shownTextPanel = TextPanel(style)

            init {
                children.add(shownTextPanel)
                shownTextPanel.backgroundRadiusCorners = CORNER_TOP_LEFT or CORNER_TOP_RIGHT
                shownTextPanel.backgroundRadius = 15f
                shownTextPanel.padding.set(10)
                shownTextPanel.instantTextLoading = true
                shownTextPanel.backgroundColor = backgroundColor
                shownTextPanel.focusBackground = backgroundColor
                shownTextPanel.focusTextColor = shownTextPanel.textColor
                shownTextPanel.breaksIntoMultiline = true
                backgroundColor = backgroundColor.mulARGB(0xffcccccc.toInt())
            }

            override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
                if (button == Key.BUTTON_LEFT && !long && numOptions < 1) {
                    stateMachine.update()
                } else super.onMouseClicked(x, y, button, long)
            }

            override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
                val idx = codepoint - '0'.code
                if (idx in 1..numOptions) {
                    questionNode?.setOutput(1, idx)
                    stateMachine.update()
                } else super.onCharTyped(x, y, codepoint)
            }

            override fun calculateSize(w: Int, h: Int) {
                super.calculateSize(w, h)
                val maxW = w * 17 / 20
                shownTextPanel.calculateSize(maxW, h)
            }

            override fun setPosition(x: Int, y: Int) {
                super.setPosition(x, y)
                val w = width * 9 / 10
                val h = height * 5 / 20
                shownTextPanel.setPosSize(x + (width - w) / 2, y + height - h, w, h)
            }

            fun drawChar(image: Texture2D, pos: Int, hasText: Boolean, w: Int) {
                val h = image.height * w / image.width
                image.bind(0)
                image.ensureFilterAndClamping(GPUFiltering.LINEAR, Clamping.CLAMP)
                drawTexture(
                    x + width * pos / 100 - w / 2,
                    y + height * (if (hasText) 7 else 9) / 10 - h / 2, w, h, image
                )
            }

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

                if (textTime == 0L) textTime = Time.gameTimeN

                val hasText = shownText.isNotBlank()

                // draw background
                val bgImage = ImageGPUCache[background, true]
                if (bgImage != null) {
                    // to do can/should we blur the background a little?
                    // to do maybe foreground, too, based on cursor?
                    val (w, h) = ImageScale.scaleMin(bgImage.width, bgImage.height, width, height)
                    val yi = if (hasText) y + (height * 8 / 10 - h) / 2 else y + (height - h) / 2
                    drawTexture(x + (width - w) / 2, yi, w, h, bgImage)
                } else drawBackground(x0, y0, x1, y1)

                val charWidth = (0.38f * max(width, height)).toInt()

                val right = ImageGPUCache[secondary, true]
                val left = ImageGPUCache[primary, true]
                when {
                    left != null && right != null -> {
                        drawChar(right, 75, hasText, charWidth)
                        drawChar(left, 25, hasText, charWidth)
                    }
                    left != null -> {
                        drawChar(left, 42, hasText, charWidth)
                    }
                    right != null -> {
                        drawChar(right, 58, hasText, charWidth)
                    }
                }

                if (hasText) {
                    drawBackground(x, y + height * 8 / 10, x + width, y + height)

                    val progress = 10 * sq(1e-9 * (Time.gameTimeN - textTime))
                    shownTextPanel.text = shownText.substring(0, min(progress.toInt(), shownText.length))

                    drawChildren(x0, y0, x1, y1)
                }
                invalidateDrawing() // for next frame
            }
        }
        splitter.add(graphPanel)
        splitter.add(gamePanel)

        testUI("Visual Novel", splitter)
    }
}