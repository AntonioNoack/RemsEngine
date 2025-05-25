package me.anno.games.visualnovel

import me.anno.graph.visual.node.Node
import me.anno.ui.editor.graph.GraphEditor
import me.anno.utils.OS

object VisualNovelState {

    var graphEditor: GraphEditor? = null

    val samples = OS.downloads.getChild("sprites")
    var background = OS.pictures.getChild("4k.jpg")
    var primary = samples.getChild("anise_c_laugh.png")
    var secondary = samples.getChild("cardamom_nc_normal.png")

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
}