package me.anno.tests.game

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.NodeLibrary.Companion.flowNodes
import me.anno.graph.types.flow.StartNode
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.graph.types.states.StateMachine
import me.anno.graph.types.states.StateNode
import me.anno.graph.ui.GraphEditor
import me.anno.graph.ui.GraphPanel
import me.anno.image.ImageGPUCache
import me.anno.image.ImageScale
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.Maths.min
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.text.TextPanel
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.style.Style
import me.anno.utils.Color.mulARGB
import me.anno.utils.OS.downloads
import me.anno.utils.OS.pictures
import kotlin.math.max

object VisualNovel {

    val samples = downloads.getChild("sprites")
    var background: FileReference = pictures.getChild("4k.jpg")
    var primary: FileReference = samples.getChild("anise_c_laugh.png")
    var secondary: FileReference = samples.getChild("cardamom_nc_normal.png")

    var shownText = "Start\n" +
            "  1. Option 1\n" +
            "  2. Option 2\n" +
            "  3. Option 3\n"
    var textTime = 0L
    var numOptions: Int = 0
    var questionNode: QuestionNode? = null

    class SpeakNode : StateNode("Speak", listOf("String", "Text"), emptyList()) {
        override fun onEnterState(oldState: StateNode?) {
            shownText = (getInput(1) ?: "").toString()
            textTime = 0
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

    class QuestionNode : StateNode("Question", listOf("String", "Question"), listOf("Int", "Result")) {
        override fun onEnterState(oldState: StateNode?) {
            questionNode = this
            textTime = 0
            val options = (2 until inputs!!.size).map { (getInput(it) ?: "").toString() }.filter { it.isNotBlank() }
            shownText = (getInput(1) ?: "").toString() + "\n" +
                    options.withIndex().joinToString("") { (idx, it) -> "  ${idx + 1}. $it\n" }
            numOptions = options.size
        }

        override fun canAddInput(type: String, index: Int) = index > 0 && type == "String"
    }

    class SceneNode : ActionNode("Scene", listOf("FileReference", "Background"), emptyList()) {
        override fun executeAction() {
            background = getInput(1) as? FileReference ?: InvalidRef
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {

        // todo story graph
        // nodes for changing character images, background, effects and such,
        // todo load good images from somewhere...

        // UI for this
        //  - background image
        //  - characters talking

        // UI split in two: graph, and game
        // todo restart button

        val graph = StateMachine()
        val library = NodeLibrary(flowNodes.nodes + listOf(
            { SpeakNode() },
            { QuestionNode() },
            { CharacterNode() },
            { SceneNode() }
        ))

        val start = object : StartNode() {
            override fun createUI(g: GraphPanel, list: PanelList, style: Style) {
                val self = this
                super.createUI(g, list, style)
                list.add(TextButton("Start", false, style)
                    .addLeftClickListener {
                        graph.start(self)
                    })
            }
        }
        graph.add(start)

        val splitter = CustomList(false, style)
        val graphPanel = GraphEditor(graph, style)
        graphPanel.library = library

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
                backgroundColor = backgroundColor.mulARGB(0xffcccccc.toInt())
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

            fun drawChar(image: Texture2D, pos: Int, w: Int) {
                val h = image.height * w / image.width
                image.bind(0)
                image.ensureFilterAndClamping(GPUFiltering.LINEAR, Clamping.CLAMP)
                drawTexture(
                    x + width * pos / 100 - w / 2,
                    y + height * 7 / 10 - h / 2, w, h, image
                )
            }

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

                if (textTime == 0L) textTime = Engine.gameTime

                // draw background
                val bgImage = ImageGPUCache[background, true]
                if (bgImage != null) {
                    // to do can/should we blur the background a little?
                    // to do maybe foreground, too, based on cursor?
                    val (w, h) = ImageScale.scaleMin(bgImage.width, bgImage.height, width, height)
                    drawTexture(x + (width - w) / 2, y + (height * 8 / 10 - h) / 2, w, h, bgImage)
                } else drawBackground(x0, y0, x1, y1)

                val charWidth = (0.38f * max(width, height)).toInt()

                val right = ImageGPUCache[secondary, true]
                val left = ImageGPUCache[primary, true]
                when {
                    left != null && right != null -> {
                        drawChar(right, 75, charWidth)
                        drawChar(left, 25, charWidth)
                    }
                    left != null -> {
                        drawChar(left, 42, charWidth)
                    }
                    right != null -> {
                        drawChar(right, 58, charWidth)
                    }
                }

                drawBackground(x, y + height * 8 / 10, x + width, y + height)

                // todo accept clicks
                val progress = 1e-9 * 10 * (Engine.gameTime - textTime)
                shownTextPanel.text = shownText.substring(0, min(progress.toInt(), shownText.length))

                drawChildren(x0, y0, x1, y1)
                invalidateDrawing() // for next frame
            }
        }
        splitter.add(graphPanel)
        splitter.add(gamePanel)

        testUI("Visual Novel", splitter)
    }
}