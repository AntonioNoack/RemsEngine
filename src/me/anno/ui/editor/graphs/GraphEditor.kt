package me.anno.ui.editor.graphs

import me.anno.studio.Studio
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelFrame
import me.anno.ui.style.Style

class GraphEditor(style: Style): PanelFrame(style) {

    // val title = TextPanel("Timeline", style)
    val body = GraphEditorBody(style)
    // val activateButtonContainer = PanelListY(style)
    val activateButton = ButtonPanel("Enable Animations", style)
        .setSimpleClickListener { Studio.selectedProperty?.isAnimated = true }

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

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val canBeActivated = Studio.selectedProperty?.isAnimated == false
        activateButton.visibility = Visibility[canBeActivated]
        super.onDraw(x0, y0, x1, y1)
    }

}