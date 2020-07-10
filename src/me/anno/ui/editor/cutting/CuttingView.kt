package me.anno.ui.editor.cutting

import me.anno.ui.base.ButtonPanel
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.style.Style

class CuttingView(style: Style): PanelListY(style){

    val addLayerView = ButtonPanel("+", style)
        .setOnClickListener { _, _, button, _ ->
            if(button == 0){
                addLayerTop()
            }
        }

    val content = ScrollPanelY(style, Padding(1), AxisAlignment.MIN)
    val layers = content.child as PanelListY
    init {
        this += TextPanel("Cutting Panel", style)
        this += content
        content.setWeight(1f)
        layers += addLayerView
    }

    fun createLayer(): Panel {
        // todo separate audio and video layers?
        // todo split them in half?
        // todo display video, audio, title, both, all?
        return TextPanel("this will be a layer...", style)
    }

    fun addLayerTop(){
        layers.children.add(1, createLayer())
    }

    fun addLayerBottom(){
        layers.children.add(createLayer())
    }

}