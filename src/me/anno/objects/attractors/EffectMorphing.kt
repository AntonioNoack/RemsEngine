package me.anno.objects.attractors

import me.anno.animation.AnimatedProperty
import me.anno.config.DefaultConfig
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.objects.Transform
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style

class EffectMorphing : Transform() {

    var lastInfluence = 0f
    val influence = AnimatedProperty.float(1f)
    val sharpness = AnimatedProperty.float(20f)

    override val className get() = "EffectMorphing"
    override val defaultDisplayName get() = Dict["Effect: Morphing", "obj.effect.morphing"]
    override val symbol get() = DefaultConfig["ui.symbol.fx.morphing", "\uD83D\uDCA0"]

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        val fx = getGroup("Effect", "", "effects")
        fx += vi("Strength", "The effective scale", influence, style)
        fx += vi("Sharpness", "How sharp the lens effect is", sharpness, style)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "influence", influence)
        writer.writeObject(this, "sharpness", sharpness)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "influence" -> influence.copyFrom(value)
            "sharpness" -> sharpness.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

}