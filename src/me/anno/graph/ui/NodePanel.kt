package me.anno.graph.ui

import me.anno.config.DefaultStyle.black
import me.anno.graph.Node
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.TextInput
import me.anno.ui.input.TextInputML
import me.anno.ui.style.Style

class NodePanel(val node: Node, style: Style) : PanelListY(style) {

    // todo round edges

    val titlePanel = TextInput("Name", "", node.name, style)

    // todo we could move the description to be a comment,
    // may be cleaner
    val descPanel = TextInputML("", node.description, style)

    // for general settings
    val generalSection = PanelListY(style)

    val inOut = PanelListX(style)

    val inputPanels = PanelListY(style)

    val outputPanels = PanelListY(style)

    var canBeResized = true
    var manualWidth = 0
    var manualHeight = 0

    init {
        add(titlePanel)
        // add(descPanel)
        add(generalSection)
        add(inOut)
        inOut.add(inputPanels.setWeight(1f))
        inOut.add(outputPanels.setWeight(1f))
        for (con in node.inputs ?: emptyArray()) {
            val panel = TextPanel("○ ${con.name}", style)
            panel.setTooltip(con.description)
            // todo add text panel with padding left for the circle
            // todo make editable, if not flow
            inputPanels.add(panel)
        }
        for (con in node.outputs ?: emptyArray()) {
            val panel = TextPanel("${con.name} ○", style)
            panel.setTooltip(con.description)
            panel.alignmentX = AxisAlignment.MAX
            // todo add text panel with padding right for the circle
            // todo right alignment
            // todo show the value, if in debugger, or sth like that
            outputPanels.add(panel)
        }
        outputPanels.alignmentX = AxisAlignment.MAX
        backgroundColor = black or (Math.random() * (1 shl 24)).toInt()
    }

}