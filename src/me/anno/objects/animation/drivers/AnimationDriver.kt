package me.anno.objects.animation.drivers

import me.anno.gpu.GFX
import me.anno.io.Saveable
import me.anno.objects.Inspectable
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.Panel
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style

// todo convert a section of the driver to keyframes...

abstract class AnimationDriver: Saveable(), Inspectable {

    abstract fun getValue(time: Float): Float
    override fun getApproxSize() = 10
    override fun isDefaultValue() = false
    abstract fun createInspector(transform: Transform, style: Style): List<Panel>
    fun show(toShow: AnimatedProperty<*>?){
        GFX.selectedProperty = toShow
    }

    abstract fun getDisplayName(): String

    // requires, that an object is selected
    override fun createInspector(list: PanelListY, style: Style) {
        list += TextPanel("Driver Inspector", style)
        for(child in createInspector(GFX.selectedTransform!!, style)){
            list += child
        }
    }

    companion object {
        fun openDriverSelectionMenu(x: Int, y: Int, oldDriver: AnimationDriver?, whenSelected: (AnimationDriver?) -> Unit){
            fun add(create: () -> AnimationDriver) = { button: Int, isLong: Boolean ->
                if(true){
                    whenSelected(create())
                    true
                } else false
            }
            val options = arrayListOf(
                "Harmonics" to add { HarmonicDriver() },
                "Noise" to add { PerlinNoiseDriver() },
                "Custom" to add { CustomDriver() }
            )
            if(oldDriver != null){
                options.add(0, "Customize" to { button, isLong ->
                    GFX.selectedInspectable = oldDriver
                    true
                })
                options += "Remove Driver" to { button, isLong ->
                    // todo confirm???
                    whenSelected(null)
                    true
                }
            }
            GFX.openMenu(x, y, if(oldDriver == null) "Add Driver" else "Change Driver", options)
        }
    }

}