package me.anno.ui.editor.cutting

import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.style.Style

class CuttingView(style: Style): PanelListY(style){

    // todo display all elements in their container
    // todo make a draggable version, so the tracks can be picked up and placed easily

    val addLayerView = ButtonPanel("+", style)
        .setSimpleClickListener { addLayerTop() }

    val content = ScrollPanelY(style, Padding(0), AxisAlignment.MIN)
    val layers = content.child as PanelListY
    init {
        // this += TextPanel("Cutting Panel", style)
        this += content
        content.setWeight(1f)
        layers += addLayerView
        addLayerTop()
        addLayerTop()
    }

    fun createLayer(): LayerView {
        // todo separate audio and video layers?
        // todo split them in half?
        // todo display video, audio, title, both, all?
        return LayerView(style)
    }

    fun addLayerTop(): LayerView {
        val v = createLayer()
        layers.children.add(1, v)
        layers.children.forEachIndexed { index, it ->
            (it as? LayerView)?.timelineSlot = index-1
        }
        return v
    }

}