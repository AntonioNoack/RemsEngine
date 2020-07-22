package me.anno.objects.rendering

import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.Studio.project
import me.anno.studio.Studio.targetDuration
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

object RenderSettings : Transform(){

    override fun getDefaultDisplayName(): String = "Render Settings"

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list.clear()
        list += TextPanel(getDefaultDisplayName(), style)
        list += VI("Duration", "Video length in seconds", AnimatedProperty.Type.FLOAT_PLUS, targetDuration, style){ project!!.targetDuration = it; save() }
    }

    fun save(){

    }

}