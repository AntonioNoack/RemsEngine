package me.anno.studio.rems

import me.anno.io.ISaveable
import me.anno.io.find.PropertyFinder
import me.anno.objects.Transform
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.inspectable.Inspectable
import me.anno.studio.rems.RemsStudio.root
import org.apache.logging.log4j.LogManager

object Selection {

    // todo make it possible to select multiple stuff
    // todo to edit common properties of all selected members <3 :D

    private val LOGGER = LogManager.getLogger(Selection::class)

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

        if (transform != null){
            val loi = transform.listOfInheritance.toList()
            if(loi.withIndex().any { (index, t) -> index > 0 && t.areChildrenImmutable }) {
                val lot = loi.last { it.areChildrenImmutable }
                LOGGER.info("Selected immutable element ${transform.name}, selecting the parent ${lot.name}")
                select(lot, null)
                return
            }
        }

        if (st == transform && sp == property) return
        val newName = if (transform == null || property == null) null else PropertyFinder.getName(transform, property)
        val propName = newName ?: selectedPropName
        RemsStudio.largeChange("Select ${transform?.name ?: "Nothing"}:$propName") {
            selectedUUID = transform?.uuid ?: -1
            selectedPropName = propName
            st = transform
            val property2 =
                if (transform == null) property else PropertyFinder.getValue(transform, selectedPropName ?: "")
            si = property2 as? Inspectable ?: transform
            sp = property2 as? AnimatedProperty<*>
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