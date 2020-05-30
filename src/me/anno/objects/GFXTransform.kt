package me.anno.objects

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import me.anno.utils.clamp

abstract class GFXTransform(parent: Transform?): Transform(parent){

    var isBillboard = AnimatedProperty<Float>(AnimatedProperty.Type.FLOAT)

    override fun createInspector(list: PanelListY, style: Style) {
        super.createInspector(list, style)
        list += FloatInput(list.style, "Alignment with Camera", AnimatedProperty.Type.FLOAT)
            .setChangeListener { putValue(isBillboard, clamp(it, 0f, 1f)) }
            .setIsSelectedListener { show(isBillboard) }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "isBillboard", isBillboard)
    }

    override fun readObject(name: String, value: Saveable?) {
        when(name){
            "isBillboard" -> {
                if(value is AnimatedProperty<*>){
                    isBillboard = value as AnimatedProperty<Float>
                }
            }
            else -> super.readObject(name, value)
        }
    }

}