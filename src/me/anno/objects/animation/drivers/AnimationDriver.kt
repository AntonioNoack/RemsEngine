package me.anno.objects.animation.drivers

import me.anno.gpu.GFX
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Inspectable
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.studio.rems.Selection.select
import me.anno.studio.rems.Selection.selectProperty
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style

// todo convert a section of the driver to keyframes...

abstract class AnimationDriver : Saveable(), Inspectable {

    var frequency = 1.0
    var amplitude = AnimatedProperty.float(1f)

    fun getValue(time: Double) = getValue0(time * frequency) * amplitude[time]

    abstract fun getValue0(time: Double): Double
    override fun getApproxSize() = 5
    override fun isDefaultValue() = false

    open fun createInspector(
        list: MutableList<Panel>,
        transform: Transform,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        list += transform.vi("Amplitude", "Scale of randomness", amplitude, style)
        list += transform.vi("Frequency", "How fast it's changing", Type.DOUBLE, frequency, style) { frequency = it }
    }

    fun show(toShow: AnimatedProperty<*>?) {
        select(selectedTransform!!, toShow)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeDouble("frequency", frequency, true)
        writer.writeObject(this, "amplitude", amplitude)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "amplitude" -> amplitude.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "frequency" -> frequency = value
            else -> super.readDouble(name, value)
        }
    }

    abstract fun getDisplayName(): String

    // requires, that an object is selected
    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        list += TextPanel("Driver Inspector", style)
        createInspector(list.children, selectedTransform!!, style, getGroup)
    }

    companion object {
        fun openDriverSelectionMenu(oldDriver: AnimationDriver?, whenSelected: (AnimationDriver?) -> Unit) {
            fun add(create: () -> AnimationDriver): () -> Unit = { whenSelected(create()) }
            val options = arrayListOf(
                GFX.MenuOption("Harmonics", "sin(pi*i*t)", add { HarmonicDriver() }),
                GFX.MenuOption("Noise", "Perlin Noise, Randomness", add { PerlinNoiseDriver() }),
                GFX.MenuOption("Custom", "Specify your own formula", add { FunctionDriver() })
            )
            if (oldDriver != null) {
                options.add(0, GFX.MenuOption("Customize", "Change the driver properties"){
                    selectProperty(oldDriver)
                })
                options += GFX.MenuOption("Remove Driver", "Changes back to keyframe-animation"){
                    whenSelected(null)
                }
            }
            GFX.openMenu(if (oldDriver == null) "Add Driver" else "Change Driver", options)
        }
    }

}