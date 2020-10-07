package me.anno.ui.editor.graphs

import me.anno.studio.RemsStudio.selectedProperty
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelFrame
import me.anno.ui.style.Style

class GraphEditor(style: Style): PanelFrame(style) {

    // val title = TextPanel("Timeline", style)
    val body = GraphEditorBody(style)
    // val activateButtonContainer = PanelListY(style)
    val activateButton = ButtonPanel("Enable Animation", style)
        .setSimpleClickListener { selectedProperty?.isAnimated = true }

    init {
        // activateButtonContainer += activateButton
        // activateButtonContainer.setWeight(1f)
        activateButton += WrapAlign.Center
    }

    init {
        // this += title
        this += body.setWeight(1f)
        this += activateButton
    }

    override fun tickUpdate() {
        super.tickUpdate()
        val canBeActivated = selectedProperty?.isAnimated == false
        activateButton.visibility = Visibility[canBeActivated]
    }

}