package me.anno.objects.attractors

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style

class UVAttractor: Transform() {

    var lastInfluence = 0f
    val influence = AnimatedProperty.float(0f)
    val sharpness = AnimatedProperty.float(1f)

    override fun getClassName() = "UVAttractor"

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val fx = getGroup("Effect", "effect")
        fx += VI("Influence", "The effective scale", influence, style)
        fx += VI("Sharpness", "How sharp the lens effect is", sharpness, style)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "influence", influence)
        writer.writeObject(this, "sharpness", sharpness)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "influence" -> influence.copyFrom(value)
            "sharpness" -> sharpness.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

}