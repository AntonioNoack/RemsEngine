package me.anno.ui.editor.cutting

import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.style.Style

class CuttingView(style: Style): ScrollPanelY(Padding(0), AxisAlignment.MIN, style){

    private val addLayerView = TextButton("+", true, style)
        .setSimpleClickListener { addLayerTop() }

    private val content = this
    private val layers = content.child as PanelListY
    init {
        content.setWeight(1f)
        layers += addLayerView
        for(i in 0 until LayerView.defaultLayerCount){
            addLayerTop()
        }
    }

    fun createLayer(): LayerView {
        // todo separate audio and video layers?
        // todo split them in half?
        // todo display video, audio, title, both, all?
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

}