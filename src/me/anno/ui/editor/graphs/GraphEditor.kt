package me.anno.ui.editor.graphs

import me.anno.gpu.GFX
import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

class GraphEditor(style: Style): PanelListY(style) {

    // val title = TextPanel("Timeline", style)
    val body = GraphEditorBody(style)
    val activateButtonContainer = PanelListY(style)
    val activateButton = ButtonPanel("Enable Animations", style)
        .setOnClickListener { _, _, _, _ -> GFX.selectedProperty?.isAnimated = true }

    init {
        activateButtonContainer += activateButton
        activateButtonContainer.setWeight(1f)
        activateButton += WrapAlign.Center
    }

    init {
        // this += title
        this += body.setWeight(1f)
        this += activateButton
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val canBeActivated = GFX.selectedProperty?.isAnimated == false
        activateButtonContainer.visibility = Visibility[canBeActivated]
        super.draw(x0, y0, x1, y1)
    }

}