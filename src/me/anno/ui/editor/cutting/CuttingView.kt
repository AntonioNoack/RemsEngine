package me.anno.ui.editor.cutting

import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.style.Style

class CuttingView(style: Style) : ScrollPanelY(Padding(0), AxisAlignment.MIN, style) {

    private val addLayerView = TextButton("+", true, style)
        .setSimpleClickListener { addLayerTop() }

    private val content = this
    private val layers = content.child as PanelListY

    init {
        content.setWeight(1f)
        layers += addLayerView
        for (i in 0 until LayerView.defaultLayerCount) {
            addLayerTop()
        }
    }

    fun createLayer(): LayerView {
        // todo display audio, name?
        // done: video
        return LayerView(style)
    }

    fun addLayerTop(): LayerView {
        val v = createLayer()
        v.parent = layers
        v.cuttingView = this
        layers.children.add(0, v)
        layers.children.forEachIndexed { index, it ->
            (it as? LayerView)?.timelineSlot = index
        }
        return v
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        for ((index, panel) in layers.children.withIndex()) {
            panel as? LayerView ?: continue
            panel.timelineSlot = index
        }
    }

}