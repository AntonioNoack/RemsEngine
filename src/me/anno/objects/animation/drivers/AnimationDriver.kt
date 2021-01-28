package me.anno.objects.animation.drivers

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.objects.inspectable.Inspectable
import me.anno.studio.rems.Selection.select
import me.anno.studio.rems.Selection.selectProperty
import me.anno.studio.rems.Selection.selectedTransform
import me.anno.ui.base.Panel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style

abstract class AnimationDriver : Saveable(), Inspectable {

    var frequency = 1.0
    var amplitude = AnimatedProperty.float(1f)

    fun getValue(time: Double, keyframeValue: Double) =
        getValue0(time * frequency, keyframeValue) * amplitude[time]

    abstract fun getValue0(time: Double, keyframeValue: Double): Double
    override fun getApproxSize() = 5
    override fun isDefaultValue() = false

    open fun createInspector(
        list: MutableList<Panel>,
        transform: Transform,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        list += transform.vi(
            "Amplitude",
            "Scale of randomness", "driver.amplitude",
            amplitude, style
        )
        list += transform.vi(
            "Frequency",
            "How fast it's changing", "driver.frequency",
            Type.DOUBLE, frequency, style
        ) { frequency = it }
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
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        list += TextPanel(
            Dict["Driver Inspector", "driver.inspector.title"],
            style
        )
        createInspector(list.children, selectedTransform!!, style, getGroup)
    }

    companion object {
        fun openDriverSelectionMenu(oldDriver: AnimationDriver?, whenSelected: (AnimationDriver?) -> Unit) {
            fun add(create: () -> AnimationDriver): () -> Unit = { whenSelected(create()) }
            val options = arrayListOf(
                MenuOption(NameDesc("Harmonics", "sin(pi*i*t)", "obj.driver.harmonics"), add { HarmonicDriver() }),
                MenuOption(
                    NameDesc("Noise", "Perlin Noise, Randomness", "obj.driver.noise"),
                    add { PerlinNoiseDriver() }),
                MenuOption(
                    NameDesc("Custom", "Specify your own formula", "obj.driver.custom"),
                    add { FunctionDriver() })
            )
            if (oldDriver != null) {
                options.add(0,
                    MenuOption(NameDesc("Customize", "Change the driver properties", "driver.edit")) {
                        selectProperty(oldDriver)
                    })
                options += MenuOption(
                    NameDesc("Remove Driver", "Changes back to keyframe-animation", "driver.remove")
                ) { whenSelected(null) }
            }
            openMenu(
                if (oldDriver == null) NameDesc("Add Driver", "", "driver.add")
                else NameDesc("Change Driver", "", "driver.change"),
                options
            )
        }
    }

}