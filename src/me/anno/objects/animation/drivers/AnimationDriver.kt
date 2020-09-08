package me.anno.objects.animation.drivers

import me.anno.gpu.GFX
import me.anno.input.MouseButton
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Inspectable
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.Studio
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

// todo convert a section of the driver to keyframes...

abstract class AnimationDriver: Saveable(), Inspectable {

    var frequency = 1.0
    var amplitude = AnimatedProperty.float(1f)

    fun getValue(time: Double) = getValue0(time * frequency) * amplitude[time]

    abstract fun getValue0(time: Double): Double
    override fun getApproxSize() = 5
    override fun isDefaultValue() = false

    open fun createInspector(list: MutableList<Panel>, transform: Transform, style: Style){
        list += transform.VI("Amplitude", "", amplitude, style)
        list += transform.VI("Frequency", "", AnimatedProperty.Type.DOUBLE, frequency, style){ frequency = it }
    }

    fun show(toShow: AnimatedProperty<*>?){
        Studio.selectedProperty = toShow
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeDouble("frequency", frequency, true)
        writer.writeObject(this, "amplitude", amplitude)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "amplitude" -> amplitude.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun readDouble(name: String, value: Double) {
        when(name){
            "frequency" -> frequency = value
            else -> super.readDouble(name, value)
        }
    }

    abstract fun getDisplayName(): String

    // requires, that an object is selected
    override fun createInspector(list: PanelListY, style: Style) {
        list += TextPanel("Driver Inspector", style)
        createInspector(list.children, Studio.selectedTransform!!, style)
    }

    companion object {
        fun openDriverSelectionMenu(x: Int, y: Int, oldDriver: AnimationDriver?, whenSelected: (AnimationDriver?) -> Unit){
            fun add(create: () -> AnimationDriver): () -> Unit = { whenSelected(create()) }
            val options = arrayListOf(
                "Harmonics" to add { HarmonicDriver() },
                "Noise" to add { PerlinNoiseDriver() },
                "Custom" to add { CustomDriver() }
            )
            if(oldDriver != null){
                options.add(0, "Customize" to {
                    Studio.selectedInspectable = oldDriver
                })
                options += "Remove Driver" to {
                    // todo make a save point
                    whenSelected(null)
                }
            }
            GFX.openMenu(x, y, if(oldDriver == null) "Add Driver" else "Change Driver", options)
        }
    }

}