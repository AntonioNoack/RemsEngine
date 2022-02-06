package me.anno.ui.editor.cutting

import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.style.Style

class LayerViewContainer(style: Style) : ScrollPanelY(Padding(0), AxisAlignment.MIN, style) {

    // todo this could be more beautiful, maybe automatically managed or sth like that...
    private val addLayerButton = TextButton("+", true, style)
        .addLeftClickListener { addLayer() }

    private val content = this
    private val layers = content.child as PanelListY

    init {
        content.setWeight(1f)
        layers += addLayerButton
        for (i in 0 until LayerView.defaultLayerCount) {
            addLayer()
        }
    }

    private fun addLayer() {
        layers.children.remove(addLayerButton)
        val v = LayerView(layers.children.size, style)
        v.parent = layers
        v.cuttingView = this
        layers.children.add(v)
        layers.children.add(addLayerButton)
    }

    override val className: String = "LayerViewContainer"

}