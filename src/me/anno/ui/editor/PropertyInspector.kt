package me.anno.ui.editor

import me.anno.language.translation.Dict
import me.anno.objects.inspectable.Inspectable
import me.anno.studio.rems.Selection.selectedInspectable
import me.anno.ui.base.Panel
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.ColorInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.TextInputML
import me.anno.ui.input.VectorInput
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style

class PropertyInspector(style: Style):
    ScrollPanelY(Padding(3), AxisAlignment.MIN, style.getChild("propertyInspector")){

    val list = child as PanelListY
    val secondaryList = PanelListY(style)
    var lastSelected: Inspectable? = null
    private var needsUpdate = false

    fun invalidate(){
        // println("updating property inspector")
        needsUpdate = true
    }

    override fun getLayoutState(): Any? = selectedInspectable

    override fun tickUpdate() {
        super.tickUpdate()
        val selected = selectedInspectable
        if(selected != lastSelected){
            lastSelected = selected
            needsUpdate = false
            list.clear()
            if(selected != null){
                createInspector(selected, list, style)
            }
        } else if(needsUpdate){
            invalidateDrawing()
            lastSelected = selected
            needsUpdate = false
            secondaryList.clear()
            if(selected != null){
                createInspector(selected, secondaryList, style)
            }
            // is matching required? not really
            val src = secondaryList.listOfAll.iterator()
            val dst = list.listOfAll.iterator()
            // works as long as the structure stays the same
            while(src.hasNext() && dst.hasNext()){
                val s = src.next()
                val d = dst.next()
                when(s){
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
            if(src.hasNext() != dst.hasNext()){
                // we (would?) need to update the structure...
            }
        }
    }

    operator fun plusAssign(panel: Panel){
        list += panel
    }

    companion object {
        fun createInspector(ins: Inspectable, list: PanelListY, style: Style){
            val groups = HashMap<String, SettingCategory>()
            ins.createInspector(list, style){ title, description, dictSubPath ->
                groups.getOrPut(dictSubPath){
                    val group = SettingCategory(Dict[title, "obj.$dictSubPath"], style)
                    list += group
                    group
                }.apply {
                    if(tooltip?.isNotBlank() != true){
                        tooltip = Dict[description, "obj.$dictSubPath.desc"]
                    }
                }
            }
        }
    }

}