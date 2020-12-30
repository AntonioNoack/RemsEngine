package me.anno.objects.particles.forces

import me.anno.config.DefaultConfig
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.InspectableAnimProperty
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import org.joml.Vector3f

abstract class ForceField(val displayName: String, val description: String): Transform() {

    val strength = AnimatedProperty.vec3(Vector3f(1f))

    override fun getSymbol() = DefaultConfig["ui.symbol.forceField", "⇶"]
    override fun getDefaultDisplayName(): String = displayName

    abstract fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f

    open fun listProperties() = listOf(InspectableAnimProperty(strength, "Strength", "How much effect this force has"))

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        super.createInspector(list, style, getGroup)
        createInspector(getGroup("Force Field", "forces").content, style)
    }

    fun createInspector(list: PanelList, style: Style) {
        for(property in listProperties()){
            list += vi(property.title, property.description, property.value, style)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "strength", strength)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "strength" -> strength.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun isDefaultValue() = false
    override fun getApproxSize() = 25

}