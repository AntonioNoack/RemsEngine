package me.anno.studio.rems

import me.anno.io.ISaveable
import me.anno.io.find.PropertyFinder
import me.anno.objects.Inspectable
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.studio.rems.RemsStudio.nullCamera
import me.anno.studio.rems.RemsStudio.root

object Selection {

    // todo make it possible to select multiple stuff
    // todo to edit common properties of all selected members <3 :D

    val selectedProperty: AnimatedProperty<*>? get() = sp
    val selectedTransform: Transform? get() = st
    val selectedInspectable: Inspectable? get() = si

    var sp: AnimatedProperty<*>? = null
    var st: Transform? = null
    var si: Inspectable? = null

    var selectedUUID = -1L
    var selectedPropName: String? = null
    var needsUpdate = true

    fun select(uuid: Long, name: String?) {
        selectedUUID = uuid
        selectedPropName = name
        needsUpdate = true
    }

    fun selectProperty(property: ISaveable) {
        select(selectedTransform!!, property)
    }

    fun selectTransform(transform: Transform?) {
        select(transform, null)
    }

    fun select(transform: Transform?, property: ISaveable?) {
        if(st == transform && sp == property) return
        val propName = if (transform == null || property == null) null else PropertyFinder.getName(transform, property)
        RemsStudio.largeChange("Select ${transform?.name ?: "Nothing"}:$propName") {
            selectedUUID = transform?.uuid ?: -1
            selectedPropName = propName
            st = transform
            si = property as? Inspectable ?: transform
            sp = property as? AnimatedProperty<*>
        }
    }

    fun update() {
        if (!needsUpdate) {

            // nothing to do

        } else {

            // re-find the selected transform and property...
            st = root.listOfAll.firstOrNull { it.uuid == selectedUUID }
            val selectedTransform = st
            val selectedPropName = selectedPropName
            if (selectedTransform != null && selectedPropName != null) {
                val value = PropertyFinder.getValue(selectedTransform, selectedPropName)
                sp = value as? AnimatedProperty<*>
                si = value as? Inspectable
            } else {
                sp = null
                si = st
            }

            RemsStudio.updateSceneViews()

            needsUpdate = false

        }
    }

}