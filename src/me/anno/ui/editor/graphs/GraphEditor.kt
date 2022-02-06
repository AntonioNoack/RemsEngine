package me.anno.ui.editor.graphs

import me.anno.animation.Interpolation
import me.anno.remsstudio.Selection.selectedProperty
import me.anno.ui.base.Visibility
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.style.Style

class GraphEditor(style: Style) : PanelListY(style) {

    val controls = ScrollPanelX(style)
    val body = GraphEditorBody(style)

    init {
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
                .setTooltip(if (type.description.isEmpty()) type.displayName else "${type.displayName}: ${type.description}")
                .addLeftClickListener {
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
        controls.forAllPanels {
            it.tickUpdate()
            it.tick()
        }

        children[0].visibility = Visibility[selectedProperty?.isAnimated == true]

    }

    override val className: String = "GraphEditor"

}