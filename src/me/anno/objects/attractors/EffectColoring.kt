package me.anno.objects.attractors

import me.anno.config.DefaultConfig
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import org.joml.Vector4f

class EffectColoring: Transform() {

    var lastInfluence = 0f
    val influence = AnimatedProperty.float(1f)
    val sharpness = AnimatedProperty.float(20f)

    init { color.set(Vector4f(1f, 0f, 0f, 1f)) }

    override fun getClassName() = "EffectColoring"
    override fun getDefaultDisplayName() = Dict["Effect: Coloring", "obj.effect.coloring"]
    override fun getSymbol() = DefaultConfig["ui.symbol.fx.coloring", "\uD83C\uDFA8"]

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val fx = getGroup("Effect", "", "effect")
        fx += vi("Strength", "How much this color shall be used", influence, style)
        fx += vi("Sharpness", "How sharp the circle is", sharpness, style)
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