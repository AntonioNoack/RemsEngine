package me.anno.remsstudio

import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.io.ISaveable
import me.anno.io.find.PropertyFinder
import me.anno.remsstudio.objects.Transform
import me.anno.remsstudio.objects.inspectable.Inspectable
import me.anno.remsstudio.RemsStudio.root
import me.anno.ui.editor.PropertyInspector.Companion.invalidateUI
import me.anno.utils.structures.maps.BiMap
import me.anno.utils.types.Sequences.getOrNull
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

    var selectedUUID = -1
    var selectedPropName: String? = null
    var needsUpdate = true

    fun clear() {
        selectedUUID = -1
        selectedPropName = null
        needsUpdate = true
    }

    fun select(uuid: Int, name: String?) {
        selectedUUID = uuid
        selectedPropName = name
        needsUpdate = true
    }

    fun selectProperty(property: ISaveable) {
        if (selectedProperty == property) {
            select(selectedTransform!!, null)
        } else select(selectedTransform!!, property)
    }

    fun selectTransform(transform: Transform?) {
        select(transform, null)
    }

    fun selectTransformMaybe(transform: Transform?) {
        // if already selected, don't inspect that property/driver
        if (selectedTransform == transform) clear()
        select(transform, null)
    }

    fun select(transform: Transform?, property: ISaveable?) {

        if (transform != null) {
            val loi = transform.listOfInheritance.toList()
            if (loi.withIndex().any { (index, t) -> index > 0 && t.areChildrenImmutable }) {
                val lot = loi.last { it.areChildrenImmutable }
                LOGGER.info("Selected immutable element ${transform.name}, selecting the parent ${lot.name}")
                select(lot, null)
                return
            }
        }

        if (st == transform && sp == property) return
        val newName = if (transform == null || property == null) null else PropertyFinder.getName(transform, property)
        val propName = newName ?: selectedPropName
        // LOGGER.info("$newName:$propName from ${transform?.className}:${property?.className}")
        RemsStudio.largeChange("Select ${transform?.name ?: "Nothing"}:$propName") {
            selectedUUID = getIdFromTransform(transform)
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
            st = getTransformFromId()
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

            invalidateUI()

            needsUpdate = false

        }
    }

    private fun getIdFromTransform(transform: Transform?): Int {
        var id = if (transform == null) -1 else root.listOfAll.indexOf(transform)
        // a special value
        if (transform != null && id == -1) {
            id = getSpecialUUID(transform)
        }
        return id
    }

    private fun getTransformFromId(): Transform? {
        return when {
            selectedUUID < 0 -> null
            selectedUUID < specialIdOffset -> root.listOfAll.getOrNull(selectedUUID)
            else -> specialIds.reverse.getOrDefault(selectedUUID, null)
        }
    }

    private const val specialIdOffset = 1_000_000_000
    private val specialIds = BiMap<Transform, Int>(32)
    private fun getSpecialUUID(t: Transform): Int {
        if (t in specialIds) return specialIds[t]!!
        val id = specialIds.size + specialIdOffset
        specialIds[t] = id
        return id
    }

}