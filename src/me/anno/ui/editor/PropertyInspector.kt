package me.anno.ui.editor

import me.anno.language.translation.Dict
import me.anno.objects.inspectable.Inspectable
import me.anno.ui.base.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.input.ColorInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.TextInputML
import me.anno.ui.input.VectorInput
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style
import me.anno.utils.types.Strings.isBlank2

class PropertyInspector(val getInspectables: () -> List<Inspectable>, style: Style) :
    ScrollPanelY(Padding(3), AxisAlignment.MIN, style.getChild("propertyInspector")) {

    constructor(getInspectable: () -> Inspectable?, style: Style, ignored: Unit) :
            this({ getInspectable().run { if (this == null) emptyList() else listOf(this) } }, style)

    val list = child as PanelListY
    val secondaryList = PanelListY(style)
    var lastSelected: List<Inspectable> = emptyList()
    private var needsUpdate = false

    fun invalidate() {
        needsUpdate = true
    }

    override fun getLayoutState(): List<Inspectable> = getInspectables()

    override fun tickUpdate() {
        super.tickUpdate()
        val selected = getInspectables()
        if (selected != lastSelected) {
            lastSelected = selected
            needsUpdate = false
            list.clear()
            if (selected.isNotEmpty()) {
                createInspector(selected, list, style)
            }
        } else if (needsUpdate) {
            invalidateDrawing()
            lastSelected = selected
            needsUpdate = false
            secondaryList.clear()
            if (selected.isNotEmpty()) {
                createInspector(selected, secondaryList, style)
            }
            // is matching required? not really
            val src = secondaryList.listOfAll.iterator()
            val dst = list.listOfAll.iterator()
            // works as long as the structure stays the same
            while (src.hasNext() && dst.hasNext()) {
                val s = src.next()
                val d = dst.next()
                // don't change the value while the user is editing it
                // this would cause bad user experience:
                // e.g. 0.0001 would be replaced with 1e-4
                if (d.listOfAll.any { it.isInFocus }) continue
                when (s) {
                    is FloatInput -> {
                        (d as? FloatInput)?.apply {
                            d.setValue(s.lastValue, false)
                        }
                    }
                    is VectorInput -> {
                        (d as? VectorInput)?.apply {
                            d.setValue(s, false)
                        }
                    }
                    is ColorInput -> {
                        (d as? ColorInput)?.apply {
                            d.setValue(s.getValue(), false)
                        }
                    }
                    is Checkbox -> {
                        (d as? Checkbox)?.apply {
                            d.isChecked = s.isChecked
                        }
                    }
                    is TextInputML -> {
                        (d as? TextInputML)?.apply {
                            d.setText(s.text, false)
                        }
                    }
                }
            }
            if (src.hasNext() != dst.hasNext() && selected.isNotEmpty()) {
                // we need to update the structure...
                lastSelected = emptyList()
                tickUpdate()
            }
        }
    }

    operator fun plusAssign(panel: Panel) {
        list += panel
    }

    companion object {

        fun createInspector(ins: List<Inspectable>, list: PanelListY, style: Style) {
            val groups = HashMap<String, SettingCategory>()
            ins[0].createInspector(ins, list, style) { title, description, dictSubPath ->
                groups.getOrPut(dictSubPath) {
                    val group = SettingCategory(Dict[title, "obj.$dictSubPath"], style)
                    list += group
                    group
                }.apply {
                    if (tooltip?.isBlank2() != false) {
                        tooltip = Dict[description, "obj.$dictSubPath.desc"]
                    }
                }
            }
        }

        fun createInspector(ins: Inspectable, list: PanelListY, style: Style) {
            val groups = HashMap<String, SettingCategory>()
            ins.createInspector(list, style) { title, description, dictSubPath ->
                groups.getOrPut(dictSubPath) {
                    val group = SettingCategory(Dict[title, "obj.$dictSubPath"], style)
                    list += group
                    group
                }.apply {
                    if (tooltip?.isBlank2() != false) {
                        tooltip = Dict[description, "obj.$dictSubPath.desc"]
                    }
                }
            }
        }

    }

}