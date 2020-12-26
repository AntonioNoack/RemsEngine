package me.anno.ui.editor.graphs

import me.anno.objects.animation.Interpolation
import me.anno.studio.rems.Selection.selectedProperty
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.Visibility
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.style.Style

class GraphEditor(style: Style) : PanelListY(style) {

    val controls = ScrollPanelX(style)
    val activateButton = TextButton("Enable Animation", false, style)
        .setSimpleClickListener { selectedProperty?.isAnimated = true }

    val body = GraphEditorBody(style)

    init {
        activateButton += WrapAlign.Center
        this += activateButton
        this += controls
        this += body.setWeight(1f)
        val cc = controls.child as PanelList
        for (type in Interpolation.values()) {
            cc += object : TextButton(type.symbol, true, style) {
                override fun tickUpdate() {
                    visibility = Visibility[body.selectedKeyframes.isNotEmpty()]
                    super.tickUpdate()
                }
            }
                .apply {
                    padding.left = 2
                    padding.right = 2
                    padding.top = 0
                    padding.bottom = 0
                }
                .setTooltip("${type.displayName}: ${type.description}")
                .setSimpleClickListener {
                    body.selectedKeyframes.forEach {
                        it.interpolation = type
                    }
                    body.invalidateDrawing()
                }
        }

    }

    override fun tickUpdate() {
        super.tickUpdate()

        // explicitly even for invisible children
        controls.listOfAll.forEach {
            it.tickUpdate()
            it.tick()
        }

        children[0].visibility = Visibility[selectedProperty?.isAnimated == false]
        children[1].visibility = Visibility[selectedProperty?.isAnimated == true]

    }

}