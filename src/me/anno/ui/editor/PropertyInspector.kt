package me.anno.ui.editor

import me.anno.objects.Inspectable
import me.anno.studio.RemsStudio.selectedInspectable
import me.anno.ui.base.Panel
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.ColorInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style

class PropertyInspector(style: Style):
    ScrollPanelY(Padding(3), AxisAlignment.MIN, style.getChild("propertyInspector")){

    val list = child as PanelListY
    val secondaryList = PanelListY(style)
    var lastSelected: Inspectable? = null
    private var needsUpdate = false

    fun invalidate(){
        needsUpdate = true
    }

    override fun getLayoutState(): Any? {
        return Pair(super.getLayoutState(), selectedInspectable)
    }

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
            while(src.hasNext() && dst.hasNext()){// works as long as the structure stays the same
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
                            // contentView.
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
            ins.createInspector(list, style){ title, id ->
                groups.getOrPut(id){
                    val group = SettingCategory(title, style)
                    list += group
                    group
                }
            }
        }
    }

}